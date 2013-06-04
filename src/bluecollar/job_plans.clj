(ns bluecollar.job-plans
  (:require [cheshire.core :as json]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]
            [clj-time.format :as time-parser]
            [bluecollar.union-rep :as union-rep]
            [bluecollar.redis :as redis]))

(defprotocol Schedulable
  (schedulable? [_] "Returns true or false depending on whether the implementation can be scheduled for the future.")
  (secs-to-runtime [_] "Number of seconds remaining before it is run."))

(defrecord JobPlan [worker args uuid scheduled-runtime]
  Schedulable
  (schedulable? [this] 
    (if-not (nil? (:scheduled-runtime this))
      (let [parsed-scheduled-runtime (time-parser/parse (:scheduled-runtime this))]
        (time/after? parsed-scheduled-runtime (time/now)))
      false))
  (secs-to-runtime [this] (if (schedulable? this)
                            (long (/ (- (time-coerce/to-long (time-parser/parse (:scheduled-runtime this))) (time-coerce/to-long (time/now))) 1000.0))
                            (long 0))))

(def maximum-failures
  "The maximum number of failures retry will exhaust. Re-define this threshold if you see fit."
  (atom 24))

(def delay-base
  "Serves as the base of the exponential calculation for delay calculation where the failure
   count is the exponent."
  (atom 5))

(defn generate-uuid [] (str (java.util.UUID/randomUUID)))

(defn new-job-plan
  "Instantiates a new 'job plan' for a worker to perform.
   Example: (new-job-plan :hard-worker [1 2 3])
   The worker should be specified as a keyword, just as it was registered during setup.
   The args should be specified as a Vector and must match the order and arity of the worker's function argument(s).
   The optional scheduled runtime must be specified in ISO8601 date/time string format (ie. 2013-05-25T23:40:15.011Z)." 
  ([worker args] (new-job-plan worker args nil))
  ([worker args scheduled-runtime] (new-job-plan worker args (generate-uuid) scheduled-runtime))
  ([worker args uuid scheduled-runtime] (->JobPlan (keyword worker) args uuid scheduled-runtime)))

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
  (let [failures (redis/failure-count (:uuid job-plan))
        scheduled-runtime (str (time/plus (time/now) (time/secs (retry-delay failures))))
        scheduled-job-plan (assoc job-plan :scheduled-runtime scheduled-runtime)]
    (enqueue scheduled-job-plan)))

;TODO need to remove the job plan UUID from the failed workers hash if it isn't retryable
(defn on-failure 
  "Always remove the failed job from the processing queue.
   If allowable, retry the failed job-plan, otherwise remove it's UUID from the failed workers hash."
  [job-plan]
  (redis/processing-pop (as-json job-plan))
  (let [uuid (:uuid job-plan)]
    (if (retry-on-failure? job-plan)
      (do
        (redis/failure-inc uuid)
        (retry job-plan))
      (redis/failure-delete uuid))))

(defn as-runnable [job-plan]
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
