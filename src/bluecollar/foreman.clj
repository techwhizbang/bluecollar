(ns bluecollar.foreman
  (:import java.util.concurrent.Executors)
  (:require [bluecollar.union-rep :as union-rep]
            [bluecollar.job-plans :as plan]))

(def ^:private thread-pool (atom nil))

(defn- start-pool [thread-count]
  (let [pool (. Executors newFixedThreadPool thread-count)]
    (do
      (reset! thread-pool pool)
      (.prestartAllCoreThreads @thread-pool))))

(defn- stop-pool []
  (.shutdown @thread-pool))

(defn worker-count []
  "Returns the total number of workers."
  (.getPoolSize @thread-pool))

(defn start-workers [worker-count]
  "Starts the given number of workers."
  (do
    (start-pool worker-count)))

(defn stop-workers []
  "Stops the workers gracefully by letting them complete any jobs already started."
  (do
    (stop-pool)))

(defn dispatch-worker [job-plan-fn]
  "Dispatches a job plan function to the worker pool."
  (.execute @thread-pool job-plan-fn))

(defn dispatch-work [job-plan]
  "Convert the given job plan for a worker, and dispatch a worker."
  (dispatch-worker (plan/for-worker job-plan)))



