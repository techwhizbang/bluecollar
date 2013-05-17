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
    (let [registered-worker (union-rep/find-worker worker-name)]
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


(defn- retry-on-failure? [job-plan]
  (let [retryable-job? (get job-plan :retry)
        uuid (get job-plan :uuid)]
   (and retryable-job? (<= (redis/failure-count uuid) 25))))

(defn- retry [job-plan]
  (let [uuid (get job-plan :uuid)
        fail-cnt (redis/failure-count uuid)
        retry-delay-in-secs (Math/pow 5 fail-cnt)])
  (redis/failure-inc (get job-plan :uuid))
  (enqueue job-plan))
;TODO
;on failure
;   if max failures not met
;   increment the times the job has failed based on the UUID in Redis
;   determine the scheduled time by now + delay, add to job plan
;   push back into the queue

; on receipt of a retried job 
;   if the scheduled time is <= NOW
;     dispatch worker
;   if the scheduled time is > NOW
;     schedule worker
(defn on-failure [job-plan]  
  (if (retry-on-failure?)
    (retry job-plan)))

(defn for-worker [job-plan]
  (let [worker-name (get job-plan :worker)
        registered-worker (union-rep/find-worker worker-name)
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
