(ns bluecollar.foreman
  (:import java.util.concurrent.Executors))

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
  "Stops the workers gracefully by letting them complete any tasks already started."
  (do
    (stop-pool)))

(defn dispatch-worker [func]
  "Dispatches a worker to perform a job, namely the given function."
  (.execute @thread-pool func))

