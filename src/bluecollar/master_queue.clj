(ns bluecollar.master-queue
  (:use bluecollar.lifecycle)
  (:require [bluecollar.redis :as redis]
            [bluecollar.job-plans :as plan]
            [bluecollar.workers-union :as workers-union]
            [bluecollar.keys-and-queues :as keys-qs]
            [cheshire.core :as json]
            [clojure.tools.logging :as logger]))

(defrecord MasterQueue [worker-count continue-running])

(defn- handler [value]
  (if (and (not (nil? value)) (not (coll? value)))
    (let [job-plan (plan/from-json value)
          worker (workers-union/find-worker (:worker job-plan))
          intended-queue (:queue worker)]
      (redis/push intended-queue value)
      (redis/remove-from-processing value keys-qs/master-processing-queue-name)
      )))

(extend-type MasterQueue
  Lifecycle
  
  (startup [this] 
    (logger/info "Starting the MasterQueue")
    
    (dotimes [x (:worker-count this)]
      (future
        (let [new-redis-conn (redis/new-connection)]
          (while @(:continue-running this)
            (try
              (let [queue keys-qs/master-queue-name
                    processing-queue keys-qs/master-processing-queue-name]
                (handler (redis/blocking-pop queue processing-queue 1 new-redis-conn)))
              (catch Exception ex
                (logger/error ex))))))
    ))

  (shutdown [this]
    (logger/info "Stopping the MasterQueue")
    (reset! (:continue-running this) false)
    ))

(defn new-master-queue 
  ([] (new-master-queue 1))
  ([worker-count] (->MasterQueue worker-count (atom true))))
