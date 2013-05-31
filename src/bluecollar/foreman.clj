(ns bluecollar.foreman
  (:import java.util.concurrent.Executors)
  (:require [bluecollar.union-rep :as union-rep]
            [bluecollar.job-plans :as plan]
            ))

(def ^:private thread-pool (atom nil))
(def ^:private scheduled-thread-pool (atom nil))

(def ^:private hostname (atom nil))

(defn- start-pool [thread-count]
  (let [pool (. Executors newFixedThreadPool thread-count)
        scheduled-pool (. Executors newScheduledThreadPool thread-count)]
    (reset! thread-pool pool)
    (reset! scheduled-thread-pool scheduled-pool)
    (.prestartAllCoreThreads @thread-pool)
    (.prestartAllCoreThreads @scheduled-thread-pool)))

(defn- stop-pool []
  (.shutdown @thread-pool)
  (.shutdown @scheduled-thread-pool))

(defn worker-count []
  "Returns the total number of workers."
  (.getPoolSize @thread-pool))

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

(defn dispatch-worker [job-plan-fn]
  "Dispatches a job plan function to the worker pool."
  (.execute @thread-pool job-plan-fn))

;TODO unify the signature of dispatch-scheduled-worker and dispatch-worker defns
(defn dispatch-scheduled-worker [job-plan]
  (let [scheduled-runtime (.secs-to-runtime job-plan)
        runnable-job-plan (plan/for-worker job-plan)]
    (.schedule @scheduled-thread-pool runnable-job-plan scheduled-runtime (java.util.concurrent.TimeUnit/SECONDS))))

(defn dispatch-work [job-plan]
  "Convert the given job plan for a worker, and dispatch a worker."
  (if (.schedulable? job-plan)
    (dispatch-scheduled-worker job-plan)
    (dispatch-worker (plan/for-worker job-plan))))



