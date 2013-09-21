(ns bluecollar.job-plans
  (:require [cheshire.core :as json]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]
            [clj-time.format :as time-parser]
            [bluecollar.workers-union :as workers-union]
            [bluecollar.redis :as redis]
            [bluecollar.keys-and-queues :as keys-and-qs]
            [clojure.tools.logging :as logger]))

(defprotocol Schedulable
  (schedulable? [_] "Returns true or false depending on whether the implementation can be scheduled for the future.")
  (secs-to-runtime [_] "Number of seconds remaining before it is run."))

(defrecord JobPlan [worker #^clojure.lang.PersistentVector args uuid scheduled-runtime])

(extend-type JobPlan
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
  "Instantiates a new JobPlan for a worker to perform.
   Example: (new-job-plan :hard-worker [1 2 3])
   The worker should be specified as a keyword, just as it was registered during setup.
   The args should be specified as a Vector and must match the order and arity of the worker's function argument(s).
   The optional scheduled runtime must be specified in ISO8601 date/time string format (ie. 2013-05-25T23:40:15.011Z)." 
  ([worker args] (new-job-plan worker args nil))
  ([worker args scheduled-runtime] (new-job-plan worker args (generate-uuid) scheduled-runtime))
  ([worker args uuid scheduled-runtime] (->JobPlan (keyword worker) args uuid scheduled-runtime)))

(defn as-json [job-plan]
  (let [job-plan-map {:worker (:worker job-plan)
                      :args (:args job-plan)
                      :uuid (:uuid job-plan)
                      :scheduled-runtime (time-coerce/to-string (:scheduled-runtime job-plan))}]
    (json/generate-string job-plan-map)))

(defn from-json [plan-as-json]
  (let [parsed-map (json/parse-string plan-as-json)]
    (new-job-plan (get parsed-map "worker") 
                  (get parsed-map "args") 
                  (get parsed-map "uuid") 
                  (get parsed-map "scheduled-runtime"))
    ))

(defn on-success [job-plan]
  (let [worker-name (:worker job-plan)
        registered-worker (workers-union/find-worker worker-name)
        queue (:queue registered-worker)
        job-uuid (:uuid job-plan)]
    (logger/info "Successfully executed JobPlan with UUID" job-uuid)
    
    (redis/srem (keys-and-qs/worker-set-name queue) job-uuid)
    (redis/del (keys-and-qs/worker-key queue job-uuid))
    (redis/success-total-inc)
    
    ))

(defn below-failure-threshold? [uuid]
  (< (redis/failure-retry-cnt uuid) @maximum-failures))

(defn retry-on-failure? [job-plan]
  (let [worker-name (:worker job-plan)
        registered-worker (workers-union/find-worker worker-name)
        retryable-worker? (:retry registered-worker)
        uuid (:uuid job-plan)]
   (and retryable-worker? (below-failure-threshold? uuid))))

(defn retry-delay [failures] (Math/pow @delay-base failures))

(defn async-job-plan
  ^{:doc "Push a JobPlan to the master queue to process asynchronously.
          If it successfully pushes the JobPlan it will return the UUID associated
          with the JobPlan."}
  ([worker-name #^clojure.lang.PersistentVector args]
    (async-job-plan (new-job-plan worker-name args)))
  ([worker-name args scheduled-runtime]
    (async-job-plan (new-job-plan worker-name args scheduled-runtime)))
  ([job-plan]
    (let [worker-name (:worker job-plan)
          registered-worker (workers-union/find-worker worker-name)]
    (if-not (nil? registered-worker)
      (do
        (redis/push keys-and-qs/master-queue-name (as-json job-plan))
        (:uuid job-plan))
      (throw (RuntimeException. (str worker-name " was not found in the worker registry.")))
      ))))

(defn- retry [job-plan]
  (let [uuid (:uuid job-plan)
        worker-name (:worker job-plan)
        registered-worker (workers-union/find-worker worker-name)
        failures (redis/failure-retry-cnt uuid)
        scheduled-runtime (str (time/plus (time/now) (time/secs (retry-delay failures))))
        scheduled-job-plan (assoc job-plan :scheduled-runtime scheduled-runtime)]
    (logger/info "retrying the JobPlan with UUID" uuid "at" scheduled-runtime)
    (redis/push (:queue registered-worker) (as-json scheduled-job-plan))))

(defn on-failure 
  "Increment total job failures.
   Always remove the failed job from the processing queue.
   If allowable, retry the failed job-plan, otherwise remove it's UUID from the failed workers hash."
  [job-plan]
  (let [worker-name (:worker job-plan)
        registered-worker (workers-union/find-worker worker-name)
        queue (:queue registered-worker)
        job-uuid (:uuid job-plan)]
    (logger/debug "Removing" (as-json job-plan) "from processing queue")
    ; (redis/with-transaction
      (redis/srem (keys-and-qs/worker-set-name queue) job-uuid)
      (redis/del (keys-and-qs/worker-key queue job-uuid))
      (redis/failure-total-inc)
      ; )
    (let [uuid (:uuid job-plan)]
      (if (retry-on-failure? job-plan)
        (do
          (redis/failure-retry-inc uuid)
          (retry job-plan))
        (redis/failure-retry-del uuid)))))

(defn as-runnable [job-plan]
  (let [worker-name (:worker job-plan)
        registered-worker (workers-union/find-worker worker-name)
        worker-fn (:func registered-worker)
        uuid (:uuid job-plan)
        args (:args job-plan)]
    (fn job-fn [] 
      (try
        (logger/info "executing a JobPlan with UUID:" uuid "for worker" worker-name)
        (let [worker-start-time (time/now)]
          (apply worker-fn args)
          (redis/push-worker-runtime worker-name (time/in-msecs (time/interval worker-start-time (time/now))))
          (on-success job-plan))
      (catch Exception e
        (logger/error e "there was an error when executing a JobPlan with UUID:" uuid "for worker" worker-name)
        (on-failure job-plan))
      (finally
        )))
    ))
