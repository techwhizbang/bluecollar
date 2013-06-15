(ns bluecollar.foreman
  (:use bluecollar.lifecycle)
  (:import java.util.concurrent.Executors
           java.util.concurrent.ExecutorService)
  (:require [bluecollar.union-rep :as union-rep]
            [bluecollar.job-plans :as plan]
            [clojure.tools.logging :as logger]))

(def ^:private hostname (atom nil))

(defrecord Foreman [#^int worker-count fixed-pool scheduled-pool])

(extend-type Foreman
  Lifecycle

  ;TODO start workers and then push to Redis the # of available workers and what host and queue
  ; {:host "server.xyx.123" :queue "foo bar"}  
  (startup [this]
    (logger/info "Starting up foreman")
    (let [thread-cnt (:worker-count this)
          pool (. Executors newFixedThreadPool thread-cnt)
          scheduled-pool (. Executors newScheduledThreadPool thread-cnt)]
      (reset! (:fixed-pool this) pool)
      (reset! (:scheduled-pool this) scheduled-pool)
      (.prestartAllCoreThreads @(:fixed-pool this))
      (.prestartAllCoreThreads @(:scheduled-pool this))))

  ;TODO stop workers and then delete from Redis the # of workers from this host and queue
  (shutdown [this]
    (logger/info "Shutting down foreman")
    (.shutdown @(:fixed-pool this))
    (.shutdown @(:scheduled-pool this))))

(defn new-foreman [worker-count] (->Foreman worker-count (atom nil) (atom nil)))

(defn worker-count [#^bluecollar.foreman.Foreman a-foreman]
  "Returns the total number of workers."
  (.getPoolSize @(:fixed-pool a-foreman)))

(defn scheduled-worker-count [#^bluecollar.foreman.Foreman a-foreman]
  "Returns the total number of schedulable workers"
  (.getPoolSize @(:scheduled-pool a-foreman)))

(defn dispatch-worker [#^bluecollar.foreman.Foreman a-foreman #^bluecollar.job_plans.JobPlan job-plan]
  "Dispatches a job plan to the worker pool."
  (logger/debug "The foreman is " a-foreman)
  (logger/debug "The fixed pool is " (:fixed-pool a-foreman))
  (.execute @(:fixed-pool a-foreman) (plan/as-runnable job-plan)))

(defn dispatch-scheduled-worker [#^bluecollar.foreman.Foreman a-foreman #^bluecollar.job_plans.JobPlan job-plan]
  "Dispatches a scheduled job plan to the dedicated scheduling thread pool."
  (let [scheduled-runtime (plan/secs-to-runtime job-plan)
        runnable-job-plan (plan/as-runnable job-plan)]
    (logger/debug "The foreman is " a-foreman)
    (logger/debug "The scheduled pool is " (:scheduled-pool a-foreman))
    (.schedule @(:scheduled-pool a-foreman) runnable-job-plan scheduled-runtime (java.util.concurrent.TimeUnit/SECONDS))))

(defn dispatch-work [#^bluecollar.foreman.Foreman a-foreman #^bluecollar.job_plans.JobPlan job-plan]
  "Dispatch the appropriate worker based on the given job-plan."
  (logger/info "Dispatching a worker for JobPlan with UUID: " (:uuid job-plan))
  (if (plan/schedulable? job-plan)
    (dispatch-scheduled-worker a-foreman job-plan)
    (dispatch-worker a-foreman job-plan)))



