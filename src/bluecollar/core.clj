(ns bluecollar.core
  "The core namespace for the bluecollar library. 
  Use bluecollar.core to setup and teardown bluecollar.

  In order to setup bluecollar first create two hash maps. The hash maps will contain
  the queue specifications and the worker specifications.

  As stated above, the queue specifications is a hash map.
  Each keyword in the hash map will be used as a queue name.
  Each value will determine the size of the thread pool backing the respective queue.
  It can contain any number of arbitrarily named queues.

  In this example there are 3 queue specifications:

  => {\"high-importance\" 10 \"medium-importance\" 5 \"catch-all\" 5}

  The worker specifications is also a hash map.
  Each keyword in the hash map will represent a unique worker (later this is how the worker can be referenced to enqueue jobs).
  The value for each worker specification is a hash map containing 3 required things:
    1.) The queue it should be placed on in order to be processed.
    2.) The namespace and function it should execute when being processed.
    3.) The ability to retry if the job given to the worker results in an exception.

  In this example there are 2 worker specifications:

  => { :worker-one {:fn clojure.core/+, :queue \"high-importance\", :retry true}
       :worker-two {:fn nick.zalabak/blog, :queue \"catch-all\", :retry false} }

  In order to setup bluecollar.core:

  => (use 'bluecollar.core)
  => (def queue-specs {\"high-importance\" 10 \"medium-importance\" 5 \"catch-all\" 5})
  => (def worker-specs {:worker-one {:fn clojure.core/+, :queue \"high-importance\", :retry true}
                        :worker-two {:fn nick.zalabak/blog, :queue \"catch-all\", :retry false}})
  => (bluecollar-setup queue-specs worker-specs)

  Optionally, bluecollar-setup accepts a third hash-map. The third hash-map contains connection
  details for Redis. Most likely you aren't running Redis on the same server you're running this
  application. In that scenario you'll need to provide the details on the hostname, port, db,
  timeout, and prefix. The prefix is purely a naming convention where the value is prepended to
  all of the data structures names stored in Redis.
  Here is an example using an alternative hostname, port, db, timeout, and prefix:

  => (def redis-specs {:redis-key-prefix \"my-awesome-app\",
                       :redis-hostname \"redis-master.dc1.com\",
                       :redis-port 1234,
                       :redis-db 6,
                       :redis-timeout 6000})

  => (bluecollar-setup queue-specs worker-specs redis-specs)

  In order to safely shut down bluecollar:

  => (bluecollar-teardown)

  "
  (:use bluecollar.lifecycle
        bluecollar.properties)
  (:require [bluecollar.master-queue :as master]
            [bluecollar.foreman :as foreman]
            [bluecollar.redis :as redis]
            [bluecollar.job-plans :as job-plans]
            [bluecollar.workers-union :as workers-union]
            [bluecollar.keys-and-queues :as keys-qs]
            [clojure.tools.logging :as logger]))

(def foremen (atom []))
(def master-queue (atom nil))

(defn processing-queue-recovery
  "Recovers job plans in the processing queue and places them at the front of their appropriate queue."
  [processing-queue]
  (let [incomplete-job-plan (redis/processing-pop processing-queue)]
    (if-not (nil? incomplete-job-plan)
      (do
        (let [a-job-plan (job-plans/from-json incomplete-job-plan)
              worker (:worker a-job-plan)
              queue (:queue (workers-union/find-worker worker))]
        (logger/info "Recovering this job " incomplete-job-plan)
        (redis/rpush queue incomplete-job-plan))
        (recur processing-queue))
    )))

(defn bluecollar-setup
  "Setup and start bluecollar by passing it the specifications for both the
   queues and workers."
  ([queue-specs worker-specs] (bluecollar-setup queue-specs worker-specs {:redis-hostname "127.0.0.1",
                                                                          :redis-port 6379,
                                                                          :redis-db 0,
                                                                          :redis-timeout 5000}))
  ([queue-specs worker-specs {redis-key-prefix :redis-key-prefix
                              redis-hostname :redis-hostname
                              redis-port :redis-port
                              redis-db :redis-db
                              redis-timeout :timeout
                              instance-name :instance-name}]
    (logger/info "Bluecollar setup is beginning...")
    (keys-qs/setup-prefix redis-key-prefix)
    (keys-qs/register-queues (keys queue-specs) instance-name)
    (keys-qs/register-keys)
    (redis/set-config {:host (or redis-hostname "127.0.0.1")
                       :port (or redis-port 6379)
                       :db (or redis-db 0)
                       :timeout (or redis-timeout 5000)})
    (redis/startup)
    (doseq [[worker-name worker-defn] worker-specs]
      (workers-union/register-worker worker-name
                                     (workers-union/new-unionized-worker (:fn worker-defn)
                                                                         (:queue worker-defn)
                                                                         (:retry worker-defn))))
    (processing-queue-recovery keys-qs/master-processing-queue-name)
    (processing-queue-recovery keys-qs/processing-queue-name)

    (reset! master-queue (master/new-master-queue (get queue-specs "master" 1)))

    (startup @master-queue)

    (doseq [[queue-name pool-size] (dissoc queue-specs "master")]
      (swap! foremen conj (foreman/new-foreman queue-name pool-size)))

    (doseq [a-foreman @foremen] (startup a-foreman))))

(defn bluecollar-teardown
  "Shut down bluecollar"
  []
  (logger/info "Bluecollar is being torn down...")
  (if-not (empty? @master-queue)
    (do
      (shutdown @master-queue)
      (reset! master-queue nil)))
  (if-not (empty? @foremen)
    (do
      (doseq [a-foreman @foremen] (shutdown a-foreman))
      (reset! foremen [])))
  (reset! workers-union/registered-workers {})
  (redis/shutdown))
