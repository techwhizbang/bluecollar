(ns bluecollar.master-queue
  (:use bluecollar.lifecycle)
  (:require [bluecollar.redis :as redis]
            [bluecollar.job-plans :as plan]
            [bluecollar.workers-union :as workers-union]
            [bluecollar.keys-and-queues :as keys-qs]
            [cheshire.core :as json]
            [clojure.tools.logging :as logger]))

(defrecord MasterQueue [worker-count continue-running])

(defn- handler [job-plan-json]
  (logger/debug "What is this here" job-plan-json)
  (if (and (not (nil? job-plan-json)) (not (coll? job-plan-json)))
    (let [job-plan (plan/from-json job-plan-json)
          job-uuid (:uuid job-plan)
          master-queue keys-qs/master-queue-name
          worker-set (keys-qs/worker-set-name master-queue)
          worker-key (keys-qs/worker-key master-queue job-uuid)
          worker (workers-union/find-worker (:worker job-plan))
          intended-queue (:queue worker)]
      ; processing
      (redis/sadd worker-set job-uuid)
      (redis/setex worker-key job-plan-json (* 60 60 24 7))
      ; push it to the intended queue
      (redis/push intended-queue job-plan-json)
      ; remove it from processing
      (redis/srem worker-set job-uuid)
      (redis/del worker-key))))

(extend-type MasterQueue
  Lifecycle
  
  (startup [this] 
    (logger/info "Starting the MasterQueue")
    
    (dotimes [x (:worker-count this)]
      (future
        (let [new-redis-conn (redis/new-connection)]
          (while @(:continue-running this)
            (try
              (handler (redis/brpop keys-qs/master-queue-name 1 new-redis-conn))
              (catch Exception ex
                (logger/error ex)))
            )))
    ))

  (shutdown [this]
    (logger/info "Stopping the MasterQueue")
    (reset! (:continue-running this) false)
    ))

(defn new-master-queue 
  ([] (new-master-queue 1))
  ([worker-count] (->MasterQueue worker-count (atom true))))
