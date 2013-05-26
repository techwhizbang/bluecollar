(ns bluecollar.job-plans
  (:require [cheshire.core :as json]
            [clj-time.core :as time]
            [clj-time.coerce :as time-parser]
            [bluecollar.union-rep :as union-rep]
            [bluecollar.redis-message-storage :as redis]))

(defstruct job-plan :worker :args :uuid :scheduled-runtime)

(def maximum-failures
  "The maximum number of failures retry will exhaust. Re-define this threshold if you see fit."
  (atom 24))

(def delay-base
  "Serves as the base of the exponential calculation for delay calculation where the failure
   count is the exponent."
  (atom 5))

(defn new-job-plan
  "Instantiates a new 'job plan' for a worker to perform.
   Example: (new-job-plan :hard-worker [1 2 3])
   The worker should be specified as a keyword, just as it was registered during setup.
   The args should be specified as a Vector and must match the order and arity of the worker's function argument(s).
   The optional scheduled runtime must be specified in ISO8601 date/time string format (ie. 2013-05-25T23:40:15.011Z)." 
  ([worker args] (new-job-plan worker args nil))
  ([worker args scheduled-runtime] (new-job-plan worker args (str (java.util.UUID/randomUUID)) scheduled-runtime))
  ([worker args uuid scheduled-runtime] (struct job-plan (keyword worker) args uuid scheduled-runtime)))

(defn as-json [job-plan]
  (if-let [scheduled-runtime (get job-plan :scheduled-runtime)]
    (json/generate-string (assoc job-plan :scheduled-runtime (str scheduled-runtime)))
    (json/generate-string job-plan)))

(defn from-json [plan-as-json]
  (let [parsed-map (json/parse-string plan-as-json)]
    (new-job-plan (get parsed-map "worker") (get parsed-map "args") (get parsed-map "uuid") 
                  (get parsed-map "scheduled-runtime"))
    ))

(defn enqueue
  ([worker-name args] 
    (enqueue (new-job-plan worker-name args)))
  ([worker-name args scheduled-runtime]
    (enqueue (new-job-plan worker-name args scheduled-runtime)))
  ([job-plan]
    (let [worker-name (get job-plan :worker)
          registered-worker (union-rep/find-worker worker-name)]
    (if-not (nil? registered-worker)
      (let [queue (get registered-worker :queue)]
        (redis/push queue (as-json job-plan)))
      (throw (RuntimeException. (str worker-name " was not found in the worker registry.")))
      ))))

(defn on-success [job-plan]
  (redis/processing-pop (as-json job-plan)))

(defn below-failure-threshold? [uuid]
  (< (redis/failure-count uuid) @maximum-failures))

(defn retry-on-failure? [job-plan]
  (let [worker-name (get job-plan :worker)
        registered-worker (union-rep/find-worker worker-name)
        retryable-worker? (get registered-worker :retry)
        uuid (get job-plan :uuid)]
   (and retryable-worker? (below-failure-threshold? uuid))))

(defn retry-delay [failures] (Math/pow @delay-base failures))

(defn- retry [job-plan]
  (let [uuid (get job-plan :uuid)
        _ (redis/failure-inc uuid)
        failures (redis/failure-count uuid)
        scheduled-runtime (str (time/plus (time/now) (time/secs (retry-delay failures))))
        scheduled-job-plan (assoc job-plan :scheduled-runtime scheduled-runtime)]
    (enqueue scheduled-job-plan)))

;TODO
; on receipt of a retried job 
;   if the scheduled time is <= NOW
;     dispatch worker
;   if the scheduled time is > NOW
;     schedule worker for (scheduled time - NOW) in seconds
(defn on-failure [job-plan]
  (if (retry-on-failure? job-plan)
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
