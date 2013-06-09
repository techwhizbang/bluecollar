(ns bluecollar.foreman
  (:use bluecollar.lifecycle)
  (:import java.util.concurrent.Executors
           java.util.concurrent.ExecutorService)
  (:require [bluecollar.union-rep :as union-rep]
            [bluecollar.job-plans :as plan]
            [clojure.tools.logging :as logger]))

(def ^:private fixed-thread-pool (atom nil))
(def ^:private scheduled-thread-pool (atom nil))

(def ^:private hostname (atom nil))

(defn- start-pool [thread-count]
  (let [pool (. Executors newFixedThreadPool thread-count)
        scheduled-pool (. Executors newScheduledThreadPool thread-count)]
    (reset! fixed-thread-pool pool)
    (reset! scheduled-thread-pool scheduled-pool)
    (.prestartAllCoreThreads @fixed-thread-pool)
    (.prestartAllCoreThreads @scheduled-thread-pool)))

(defn- stop-pool []
  (.shutdown @fixed-thread-pool)
  (.shutdown @scheduled-thread-pool))

(defrecord Foreman [#^int worker-count fixed-pool scheduled-pool]
  Lifecycle
  (startup [this]
    ; (reset! hostname (.getHostName (java.net.InetAddress/getLocalHost)))
    (let [thread-cnt (:worker-count this)
          pool (. Executors newFixedThreadPool thread-cnt)
          scheduled-pool (. Executors newScheduledThreadPool thread-cnt)]
      (reset! (:fixed-pool this) pool)
      (reset! (:scheduled-pool this) scheduled-pool)
      (.prestartAllCoreThreads @(:fixed-pool this))
      (.prestartAllCoreThreads @(:scheduled-pool this))))
  (shutdown [this]
    (.shutdown @(:fixed-pool this))
    (.shutdown @(:scheduled-pool this))))

(defn new-foreman [worker-count] (->Foreman worker-count (atom nil) (atom nil)))

(defn worker-count []
  "Returns the total number of workers."
  (.getPoolSize @fixed-thread-pool))

;TODO start workers and then push to Redis the # of available workers and what host and queue
; {:host "server.xyx.123" :queue "foo bar"}
(defn start-workers [worker-count]
  "Starts the given number of workers."
  (reset! hostname (.getHostName (java.net.InetAddress/getLocalHost)))
  (start-pool worker-count))

;TODO stop workers and then delete from Redis the # of workers from this host and queue
(defn stop-workers []
  "Stops the workers gracefully by letting them complete any jobs already started."
  (stop-pool))

(defn dispatch-worker [job-plan]
  "Dispatches a job plan to the worker pool."
  (.execute @fixed-thread-pool (plan/as-runnable job-plan)))

(defn dispatch-scheduled-worker [job-plan]
  "Dispatches a scheduled job plan to the dedicated scheduling thread pool."
  (let [scheduled-runtime (plan/secs-to-runtime job-plan)
        runnable-job-plan (plan/as-runnable job-plan)]
    (.schedule @scheduled-thread-pool runnable-job-plan scheduled-runtime (java.util.concurrent.TimeUnit/SECONDS))))

(defn dispatch-work [job-plan]
  "Dispatch the appropriate worker based on the given job-plan."
  (logger/info "Dispatching a worker for JobPlan with UUID: " (:uuid job-plan))
  (if (plan/schedulable? job-plan)
    (dispatch-scheduled-worker job-plan)
    (dispatch-worker job-plan)))



