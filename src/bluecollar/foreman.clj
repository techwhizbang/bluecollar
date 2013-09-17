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

(defn poll-for-work [#^bluecollar.foreman.Foreman a-foreman]
  (future
    (let [new-redis-conn (redis/new-connection)]
      (while @(:continue-running a-foreman)
        (try
          (let [value (redis/blocking-pop (:queue-name a-foreman) keys-qs/processing-queue-name 2 new-redis-conn)]
            (if (and (not (nil? value)) (not (coll? value)))
              (do-work a-foreman (plan/from-json value))))
          (catch Exception ex
            (logger/error ex)))))))

(extend-type Foreman
  Lifecycle

  (startup [this]
    (logger/info "Starting up foreman")
    (let [thread-cnt (:worker-count this)
          scheduled-pool (. Executors newScheduledThreadPool thread-cnt)]
      (reset! (:continue-running this) true)
      (reset! (:scheduled-pool this) scheduled-pool)
      (.prestartAllCoreThreads @(:scheduled-pool this))

      (dotimes [x (:worker-count this)]
        (poll-for-work this))))

  (shutdown [this]
    (logger/info "Shutting down foreman")
    (reset! (:continue-running this) false)
    (.shutdown @(:scheduled-pool this))))

(defn new-foreman [queue-name worker-count] (->Foreman queue-name worker-count (atom nil) (atom true)))

(defn scheduled-worker-count [#^bluecollar.foreman.Foreman a-foreman]
  "Returns the total number of schedulable workers"
  (.getPoolSize @(:scheduled-pool a-foreman)))



