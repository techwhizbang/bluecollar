(ns bluecollar.job-plans
  (:require [cheshire.core :as json]
    [bluecollar.union-rep :as union-rep]
    [bluecollar.redis-message-storage :as redis]))

(defstruct job-plan :worker :args :uuid)

(defn new-job-plan [worker args]
  (struct job-plan worker args (str (java.util.UUID/randomUUID))))

(defn as-json 
  ([job-plan] (json/generate-string job-plan)))

(defn from-json [plan-as-json]
  (let [parsed-map (json/parse-string plan-as-json)]
    (struct job-plan (keyword (get parsed-map "worker")) 
                     (get parsed-map "args") 
                     (get parsed-map "uuid"))
    ))

(defn enqueue
  ([worker-name args] 
    (let [registry (deref union-rep/registered-workers)
          registered-worker (get registry worker-name)]
    (if-not (nil? registered-worker)
      (let [queue (get registered-worker :queue)
            uuid (str (java.util.UUID/randomUUID))
            plan (struct job-plan (name worker-name) args uuid)]
        (redis/push queue (as-json plan)))
      (throw (RuntimeException. (str worker-name " was not found in the worker registry.")))
      )))
  ([job-plan]
    (enqueue (get job-plan :worker) (get job-plan :args))))

(defn on-success [job-plan]
  (redis/processing-pop (as-json job-plan)))

; TODO need to test the Exception case of the retry; 
; TODO also need to attach a UUID to the job and stick it into Redis to count the number of
; of retries. (str (java.util.UUID/randomUUID))
(defn on-failure [job-plan]  
  (let [retry? (get job-plan :retry)]
    (if retry?
      (enqueue job-plan))))

(defn for-worker [job-plan]
  (let [worker-registry (deref union-rep/registered-workers)
        worker-name (get job-plan :worker)
        registered-worker (get worker-registry worker-name)
        worker-fn (get registered-worker :fn)
        args (get job-plan :args)]
    (fn [] 
      (try
        (apply worker-fn args)
        (on-success job-plan)
      (catch Exception e
        (prn "caught exception: " (.getMessage e))
        (on-failure job-plan)
        )))
    ))
