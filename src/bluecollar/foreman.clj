(ns bluecollar.foreman
  (:import java.util.concurrent.Executors)
  (:require [bluecollar.labor-union-rep :as labor-rep]
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

(defn dispatch-work [job-plan-map]
  "Given the job plan map, make sure the labor union rep
   sees the worker's \"union card\", then dispatch a worker with a job plan."
  (let [worker-ns (get job-plan-map "ns")
        job-plan (plan/for-worker job-plan-map)]
    (do
      (labor-rep/union-card-check worker-ns)
      (dispatch-worker job-plan))))



