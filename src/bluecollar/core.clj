(ns bluecollar.core
  "The core namespace for the Bluecollar library. 
  Use bluecollar.core to startup and shutdown Bluecollar's backend processing.

  In order to startup Bluecollar first create two hash maps. The hash maps will contain
  the queue specifications and the worker specifications.

  As stated above, the queue specifications is a hash map.
  Each keyword in the hash map will be used as a queue name.
  Each value will determine the size of the thread pool backing the respective queue.
  It can contain any number of arbitrarily named queues. Be mindful of the number of threads allocated to each
  queue based on what your server has the capacity to handle.

  In this example there are 3 queue specifications:

  => {\"order-processing\" 10 \"fulfillment-processing\" 5 \"master\" 5}

  The worker specifications is also a hash map.
  Each keyword in the hash map will represent a unique worker (later this is how the worker can be referenced to enqueue jobs).
  The value for each worker specification is a hash map containing 3 required things:
    1.) The queue it should be placed on in order to be processed.
    2.) The namespace and function it should execute when being processed.
    3.) The ability to retry if the job given to the worker results in an exception.

  In this example there are 2 worker specifications:

  => { :worker-one {:fn order/process, :queue \"order-processing\", :retry true}
       :worker-two {:fn fulfillment/process, :queue \"fulfillment-processing\", :retry false} }

  In order to setup bluecollar.core:

  => (use 'bluecollar.core)
  => (def queue-specs {\"order-processing\" 10 \"fulfillment-processing\" 5 \"master\" 5})
  => (def worker-specs {:worker-one {:fn order/process, :queue \"order-processing\", :retry true}
                        :worker-two {:fn fulfillment/process, :queue \"fulfillment-processing\", :retry false}})
  => (bluecollar-startup queue-specs worker-specs)

  Optionally, bluecollar-startup accepts a third hash-map. The third hash-map contains connection
  details for Redis. Chances are you aren't running Redis on the same server you're running your
  application. In that scenario you'll need to provide the details on the following:
    * redis-hostname (the hostname Redis is running on)
    * redis-port (the port Redis is running on)
    * redis-db (the Redis db used)
    * redis-timeout (allowable to wait for a Redis connection)
    * redis-key-prefix (is prepended to all of the data structures stored in Redis)
    * redis-key-postfix (is appended to the end of the data structured stored in Redis)
  
  => (def redis-specs {:redis-hostname \"redis-master.dc1.com\",
                       :redis-port 1234,
                       :redis-db 6,
                       :redis-timeout 6000,
                       :redis-key-prefix \"my-awesome-app\",
                       :redis-key-postfix \"server-01\"})

  => (bluecollar-startup queue-specs worker-specs redis-specs)

  In order to safely shut down bluecollar:

  => (bluecollar-shutdown)

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

(defn processing-recovery
  "Recovers job plans left or stuck processing from a re-deploy, re-start, or a crash and places them at the front of their appropriate queue."
  [queue-name]
  (let [processing-set (keys-qs/worker-set-name queue-name)
        unfinished-job-plan-uuids (redis/smembers processing-set)]
    (doseq [uuid unfinished-job-plan-uuids]
      (let [unfinished-job-plan (redis/get-value (keys-qs/worker-key queue-name uuid))]
        (if-not (nil? unfinished-job-plan)
          (do 
            (logger/info "Recovering this job " unfinished-job-plan)
            (let [a-job-plan (job-plans/from-json unfinished-job-plan)
                  worker (:worker a-job-plan)
                  intended-queue (:queue (workers-union/find-worker worker))]
              (redis/rpush intended-queue unfinished-job-plan)
            ))))

      (redis/srem (keys-qs/worker-set-name queue-name) uuid)
      (redis/del (keys-qs/worker-key queue-name uuid))

      )))

(defn bluecollar-startup
  "Setup and start Bluecollar by passing it the specifications for both the
   queues and workers."
  ([queue-specs worker-specs] (bluecollar-startup queue-specs worker-specs {:redis-hostname "127.0.0.1",
                                                                            :redis-port 6379,
                                                                            :redis-db 0,
                                                                            :redis-timeout 5000}))
  ([queue-specs worker-specs {redis-hostname :redis-hostname
                              redis-port :redis-port
                              redis-db :redis-db
                              redis-timeout :redis-timeout
                              redis-key-prefix :redis-key-prefix
                              redis-key-postfix :redis-key-postfix}]
    (logger/info "Bluecollar is setting up...")

    (keys-qs/setup-prefix redis-key-prefix)
    (keys-qs/setup-postfix redis-key-postfix)
    (keys-qs/register-queues (keys queue-specs))
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
    (doseq [[queue-name pool-size] queue-specs]
      (processing-recovery queue-name))
    
    (reset! master-queue (master/new-master-queue (get queue-specs "master" 1)))

    (startup @master-queue)

    (doseq [[queue-name pool-size] (dissoc queue-specs "master")]
      (swap! foremen conj (foreman/new-foreman queue-name pool-size)))

    (doseq [a-foreman @foremen] (startup a-foreman))

    (logger/info "Bluecollar has started successfully.")
))

(defn bluecollar-shutdown
  "Shut down Bluecollar"
  []
  (logger/info "Bluecollar is being shut down...")
  (if-not (empty? @master-queue)
    (do
      (shutdown @master-queue)
      (reset! master-queue nil)))
  (if-not (empty? @foremen)
    (do
      (doseq [a-foreman @foremen] (shutdown a-foreman))
      (reset! foremen [])))
  (reset! workers-union/registered-workers {})
  (redis/shutdown)
  (logger/info "Bluecollar has shut down successfully."))
