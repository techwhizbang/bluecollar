(ns bluecollar.foreman
  (:use bluecollar.lifecycle)
  (:import java.util.concurrent.Executors
           java.util.concurrent.ExecutorService)
  (:require [bluecollar.workers-union :as workers-union]
            [bluecollar.job-plans :as plan]
            [bluecollar.redis :as redis]
            [bluecollar.keys-and-queues :as keys-qs]
            [clojure.tools.logging :as logger]))

(defrecord Foreman [#^String queue-name #^int worker-count scheduled-pool continue-running])

(defn dispatch-scheduled-worker [#^bluecollar.foreman.Foreman a-foreman #^bluecollar.job_plans.JobPlan job-plan]
  "Dispatches a scheduled job plan to the dedicated scheduling thread pool."
  (let [scheduled-runtime (plan/secs-to-runtime job-plan)
        runnable-job-plan (plan/as-runnable job-plan)]
    (logger/debug "The foreman is " a-foreman)
    (logger/debug "The scheduled pool is " (:scheduled-pool a-foreman))
    (.schedule @(:scheduled-pool a-foreman) runnable-job-plan scheduled-runtime (java.util.concurrent.TimeUnit/SECONDS))))

(defn do-work [#^bluecollar.foreman.Foreman a-foreman #^bluecollar.job_plans.JobPlan job-plan]
  (if (plan/schedulable? job-plan)
      (dispatch-scheduled-worker a-foreman job-plan)
      (apply (plan/as-runnable job-plan) [])))

(defn- on-receipt-handler [a-foreman job-plan-json]
  (logger/debug "on-receipt-handler" job-plan-json)
  (if (and (not (nil? job-plan-json)) (not (coll? job-plan-json)))
    (let [queue (:queue-name a-foreman)
          job-plan (plan/from-json job-plan-json)
          job-uuid (:uuid job-plan)
          worker-set (keys-qs/worker-set-name queue)]
     
      (redis/sadd worker-set job-uuid)
      ; expire in 7 days if it hasn't been processed yet
      (redis/setex (keys-qs/worker-key queue job-uuid) job-plan-json (* 60 60 24 7))
     
      (do-work a-foreman job-plan)
      )
    ))

(defn start-worker [#^bluecollar.foreman.Foreman a-foreman]
  (let [new-redis-conn (redis/new-connection)
        queue (:queue-name a-foreman)]
    (future
      (while @(:continue-running a-foreman)
        (try
          (on-receipt-handler a-foreman (redis/brpop queue 2 new-redis-conn))
          (catch Exception ex
            (logger/error ex)))))))

(extend-type Foreman
  Lifecycle

  (startup [this]
    (logger/info "Starting up foreman for queue" (:queue-name this))
    (let [thread-cnt (:worker-count this)
          scheduled-pool (. Executors newScheduledThreadPool thread-cnt)]
      (reset! (:continue-running this) true)
      (reset! (:scheduled-pool this) scheduled-pool)
      (.prestartAllCoreThreads @(:scheduled-pool this))

      (dotimes [x (:worker-count this)]
        (logger/info "Foreman starting worker" x)
        (start-worker this))))

  (shutdown [this]
    (logger/info "Shutting down foreman")
    (reset! (:continue-running this) false)
    (.shutdown @(:scheduled-pool this))))

(defn new-foreman [queue-name worker-count] (->Foreman queue-name worker-count (atom nil) (atom true)))

(defn scheduled-worker-count [#^bluecollar.foreman.Foreman a-foreman]
  "Returns the total number of schedulable workers"
  (.getPoolSize @(:scheduled-pool a-foreman)))



