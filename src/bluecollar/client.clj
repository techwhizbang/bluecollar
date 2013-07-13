(ns bluecollar.client
  "The client namespace for the bluecollar library. 
   Use bluecollar.client to both setup a client application, but
   also send asynchronous jobs to registered workers.

   In order to setup a bluecollar client application. Create a hash-map
   containing the worker specifications, just as in bluecollar.core.
   The value for each worker specification is a hash map containing 3 required things:
    1.) The queue it should be placed on in order to be processed.
    2.) The namespace and function it should execute when being processed.
    3.) The ability to retry if the job given to the worker results in an exception.

  In this example there are 2 worker specifications:

  => { :worker-one {:fn clojure.core/+, :queue \"high-importance\", :retry true}
       :fibonacci-worker {:fn fibonacci/calc, :queue \"catch-all\", :retry false} }

   => (use 'bluecollar.client)
   => (def worker-specs {:worker-one {:fn clojure.core/+, :queue \"high-importance\", :retry true}
                         :fibonacci-worker {:fn fibonacci/calc, :queue \"catch-all\", :retry false}})
   => (bluecollar-client-setup worker-specs)

   After performing bluecollar-client-setup the application can begin using
   \"async-job-for\".

   Assuming :fibonacci-worker :fn takes a single argument, namely the number of 
   Fibonacci numbers to calculate in a sequence, the client can 
   send :fibonacci-worker a job just like this:

   => (use 'bluecollar.client)
   => (async-job-for :fibonacci-worker [20])

   If you wanted the :fibonacci-worker to process the job asynchronously with a 
   specific time in the future, you could also provide an ISO-8601 formatted
   time. So instead of being processed as soon as possible, it would execute
   no sooner than 2013-06-24T23:59:59.000Z.

   => (async-job-for :fibonacci-worker [400] \"2013-06-23T21:44:32.391Z\")
   "
  (:use [bluecollar.job-plans :only [async-job-plan]]
         bluecollar.properties)
  (:require [bluecollar.redis :as redis]
            [bluecollar.union-rep :as union-rep]
            [clojure.tools.logging :as logger]))

(defn bluecollar-client-setup
  ^{:doc "Setup bluecollar for a client application."}
  ([worker-specs] (bluecollar-client-setup worker-specs {:redis-hostname "127.0.0.1",
                                                         :redis-port 6379,
                                                         :redis-db 0,
                                                         :redis-timeout 5000}))
  ([worker-specs {redis-key-prefix :redis-key-prefix
                  redis-hostname :redis-hostname
                  redis-port :redis-port
                  redis-db :redis-db
                  redis-timeout :timeout}]
    (logger/info "Bluecollar client is starting up...")
    (redis/setup-key-prefix redis-key-prefix)
    (redis/startup {:host (or redis-hostname "127.0.0.1")
                    :port (or redis-port 6379)
                    :db (or redis-db 0)
                    :timeout (or redis-timeout 5000)})
    (doseq [[worker-name worker-defn] worker-specs]
      (union-rep/register-worker worker-name 
                                (union-rep/new-worker-definition (:queue worker-defn))
                                ))))

(defn bluecollar-client-teardown 
  ^{:doc "Teardown bluecollar for a client application"}
  [] 
  (reset! redis/redis-key-prefix nil)
  (redis/shutdown)
  (reset! union-rep/registered-workers {}))

(defn async-job-for
  ^{:doc "Send a registered worker a job to process asynchronously.
          The args vector must match the arity of the function originally
          associated to the registered worker.
          Optionally a scheduled runtime can be specified as an ISO-8601 formatted string."}
  ([worker-name #^clojure.lang.PersistentVector args] 
    (async-job-plan worker-name args))
  ([worker-name #^clojure.lang.PersistentVector args scheduled-runtime]
    (async-job-plan worker-name args scheduled-runtime)))

;TODO allow the registered worker to process the job inline
; (defn inline-job-for)