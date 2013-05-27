(ns bluecollar.foreman
  (:import java.util.concurrent.Executors)
  (:require [bluecollar.union-rep :as union-rep]
            [bluecollar.job-plans :as plan]))

(def ^:private thread-pool (atom nil))
(def ^:private hostname (atom nil))

(defn- start-pool [thread-count]
  (let [pool (. Executors newFixedThreadPool thread-count)]
    (reset! thread-pool pool)
    (.prestartAllCoreThreads @thread-pool)))

(defn- stop-pool []
  (.shutdown @thread-pool))

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

(defn dispatch-work [job-plan]
  "Convert the given job plan for a worker, and dispatch a worker."
  (dispatch-worker (plan/for-worker job-plan)))



