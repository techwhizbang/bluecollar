(ns bluecollar.job-plans
  (:require [cheshire.core :as json]
    [bluecollar.union-rep :as union-rep]
    [bluecollar.redis-message-storage :as redis]))

(defstruct job-plan :worker :args)

(defn for-worker [job-plan]
  (let [worker-registry (deref union-rep/registered-workers)
        worker-name (get job-plan :worker)
        registered-worker (get worker-registry worker-name)
        worker-fn (get registered-worker :fn)
        args (get job-plan :args)]
    (fn [] (apply worker-fn args))))

(defn as-json [worker-name arg-vec]
  (json/generate-string (struct job-plan worker-name arg-vec)))

(defn from-json [plan-as-json]
  (let [parsed-map (json/parse-string plan-as-json)]
    (struct job-plan (keyword (get parsed-map "worker")) (get parsed-map "args"))
    ))

(defn enqueue [worker-name args]
  (let [registry (deref union-rep/registered-workers)
        registered-worker (get registry worker-name)]
    (if-not (nil? registered-worker)
      (redis/push (get registered-worker :queue) (as-json (name worker-name) args))
      (throw (RuntimeException. (str worker-name " was not found in the worker registry.")))
      )))
