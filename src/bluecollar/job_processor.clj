(ns bluecollar.job-processor
  (:require [bluecollar.redis-message-storage :as redis]
    [bluecollar.foreman :as foreman]
    [bluecollar.job-plans :as plan]
    [cheshire.core :as json]))

(def ^:private keep-listening (atom true))

(defn start []
  (reset! keep-listening true))

(defn- require-worker-ns [worker-ns]
  (if-not (contains? (loaded-libs) worker-ns)
            (require worker-ns)))

(defn listen [queue-name]
  (while @keep-listening
    (let [value (redis/blocking-pop queue-name)]
      (if-not (and (nil? value) (vector? value))
        (let [plan-map (plan/from-json value)
              worker-ns (get plan-map "ns")
              worker-plan (plan/for-worker plan-map)]
          (require-worker-ns worker-ns)
          (foreman/dispatch-worker worker-plan))
        ))))

(defn stop []
  (reset! keep-listening false))

