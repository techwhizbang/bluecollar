(ns bluecollar.job-plans-test
  (:use clojure.test
        bluecollar.test-helper
        conjure.core)
  (:require [clj-time.core :as time]
            [bluecollar.job-plans :as plan]
            [bluecollar.redis :as redis]
            [bluecollar.union-rep :as union-rep]
            [bluecollar.fake-worker]))

(use-fixtures :each (fn [f]
  (redis/startup redis-test-settings)
  (redis/flushdb)
  (reset! bluecollar.fake-worker/perform-called false)
  (f)))

(deftest job-plan-test
  (testing "takes a worker and a vector of args"
    (let [job-plan (plan/new-job-plan :a-worker ["a" "b"])]
      (is (= job-plan (plan/->JobPlan :a-worker, ["a" "b"] (:uuid job-plan) nil nil)))))

  (testing "takes a worker, vector of args, and a scheduled-runtime"
    (stubbing [plan/generate-uuid "random-uuid"]
      (let [now (str (time/now))
            job-plan (plan/new-job-plan :b-worker ["c" "d"] now)]
          (is (= job-plan (plan/->JobPlan :b-worker, ["c" "d"], "random-uuid", now, nil))))))

  (testing "takes a worker, vector of args, UUID, and scheduled-runtime"
    (let [uuid (str (java.util.UUID/randomUUID))
          now (str (time/now))
          job-plan (plan/new-job-plan :b-worker ["c" "d"] uuid now)]
        (is (= job-plan (plan/->JobPlan :b-worker, ["c" "d"], uuid, now, nil)))))

  (testing "takes a worker, vector of args, UUID, scheduled-runtime, and a server"
    (let [uuid (str (java.util.UUID/randomUUID))
          now (str (time/now))
          job-plan (plan/new-job-plan :b-worker ["c" "d"] uuid now "blue01.dc1")]
        (is (= job-plan (plan/->JobPlan :b-worker, ["c" "d"], uuid, now, "blue01.dc1"))))))

(deftest new-job-plan-test
  (testing "creates a new job plan with a UUID"
    (let [job-plan (plan/new-job-plan :worker [1 2 3])]
      (is (= (get job-plan :worker) :worker))
      (is (= (get job-plan :args) [1 2 3]))
      (is (not (nil? (re-find uuid-regex (get job-plan :uuid)))))
        )))

(deftest plan-as-json-test
  (testing "converts a plan to JSON without a scheduled-runtime"
    (let [job-plan (plan/new-job-plan :hard-worker [1 2] nil nil)]
      (is (= (plan/as-json job-plan)
        "{\"worker\":\"hard-worker\",\"args\":[1,2],\"uuid\":null,\"scheduled-runtime\":null,\"server\":null}")))
    )

  (testing "converts a plan to JSON with a scheduled-runtime"
    (let [now (str (time/now))
          job-plan (plan/new-job-plan :hard-worker [1 2] nil now)]
      (is (= (plan/as-json job-plan)
        (str "{\"worker\":\"hard-worker\",\"args\":[1,2],\"uuid\":null,\"scheduled-runtime\":\"" now "\",\"server\":null}")))))
  )

(deftest plan-from-json-test
  (testing "converts a plan in JSON to a map"
    (let [a-job-plan (plan/new-job-plan :hard-worker [1 2])
          a-json-plan (plan/as-json a-job-plan)]
      (is (= (plan/from-json a-json-plan) a-job-plan))
      )))

(deftest plan-as-runnable-test
  (testing "converts a plan map for a worker"
    (let [job-plan (plan/new-job-plan :hard-worker [1 2])]
      (is (fn? (plan/as-runnable job-plan)))
      ))

  (testing "makes an executable function for the worker"
    (let [workers {:hard-worker (union-rep/new-worker-definition bluecollar.fake-worker/perform
                                                                 "crunch-numbers"
                                                                 false)}
          _ (union-rep/register-workers workers)
          a-job-plan (plan/new-job-plan :hard-worker [1 2])
          _ ((plan/as-runnable a-job-plan))]
      (is (true? (deref bluecollar.fake-worker/perform-called)))
      ))
  )

(deftest on-success-test
  (testing "successfully removes a job plan from the processing queue"
    (let [processing-queue @redis/processing-queue
          job-plan (plan/new-job-plan :hard-worker [1 3])
          _ (redis/push processing-queue (plan/as-json job-plan))
          current-vals (redis/lrange processing-queue 0 0)
          _ (plan/on-success job-plan)
          remaining-vals (redis/lrange processing-queue 0 0)]
        (is (not (empty? current-vals)))
        (is (empty? remaining-vals))
      ))
  (testing "always increments the total successes counter"
    (let [_ (redis/success-total-del)
          job-plan (plan/new-job-plan :hard-worker [1 3])
          _ (plan/on-success job-plan)]
      (is (= 1 (redis/success-total-cnt))))))

(deftest retry-delay-test
  (testing "calculates 5 seconds of delay for 1 failure"
    (is (= 5.0 (plan/retry-delay 1))))
  (testing "calculates 25 seconds of delay for 2 failures"
    (is (= 25.0 (plan/retry-delay 2))))
  (testing "calculate 125 seconds of delays for 3 failures"
    (is (= 125.0 (plan/retry-delay 3)))))

(deftest below-failure-threshold-test
  (testing "returns true when it is below the threshold"
    (stubbing [redis/failure-retry-cnt (- (deref plan/maximum-failures) 1)]
      (is (= true (plan/below-failure-threshold? "foo")))))

  (testing "returns false when it is above the threshold"
    (stubbing [redis/failure-retry-cnt (+ (deref plan/maximum-failures) 1)]
      (is (= false (plan/below-failure-threshold? "foo"))))))

(deftest retry-on-failure-test
  (testing "returns false if the registered worker is not retryable"
    (let [workers {:hard-worker (union-rep/new-worker-definition bluecollar.fake-worker/perform
                                                                 "crunch-numbers"
                                                                 false)}
          _ (union-rep/register-workers workers)
          job-plan (plan/new-job-plan :hard-worker [1 2])]
      (stubbing [redis/failure-retry-cnt (- (deref plan/maximum-failures) 1)]
        (is (= false (plan/retry-on-failure? job-plan))))))

  (testing "returns false if the number of failures is above the threshold"
    (let [workers {:hard-worker (union-rep/new-worker-definition bluecollar.fake-worker/perform
                                                                 "crunch-numbers"
                                                                 true)}
          _ (union-rep/register-workers workers)
          job-plan (plan/new-job-plan :hard-worker [1 2])]
      (stubbing [redis/failure-retry-cnt (+ (deref plan/maximum-failures) 1)]
        (is (= false (plan/retry-on-failure? job-plan))))))

  (testing "returns true if the registered worker is retryable and failures are below the threshold"
    (let [workers {:hard-worker (union-rep/new-worker-definition bluecollar.fake-worker/perform
                                                                 "crunch-numbers"
                                                                 true)}
          _ (union-rep/register-workers workers)
          job-plan (plan/new-job-plan :hard-worker [1 2])]
      (stubbing [redis/failure-retry-cnt (- (deref plan/maximum-failures) 1)]
        (is (= true (plan/retry-on-failure? job-plan))))))
  )

(deftest on-failure-test
  (testing "re-enqueues the job plan when the worker allows retries"
    (let [now-ish (time/now)]
      (stubbing [plan/retry-on-failure? true
                 time/now now-ish]
        (let [workers {:hard-worker (union-rep/new-worker-definition bluecollar.fake-worker/perform
                                                                     "crunch-numbers"
                                                                    true)}
              _ (union-rep/register-workers workers)
              job-plan-original (plan/new-job-plan :hard-worker [123])
              job-plan-to-retry (assoc job-plan-original :scheduled-runtime (str (time/plus now-ish (time/secs (deref plan/delay-base)))))  
              _ (plan/on-failure job-plan-original)]
          (is (= job-plan-to-retry (plan/from-json (redis/pop-to-processing "crunch-numbers"))))
          (is (not (nil? (redis/remove-from-processing (plan/as-json job-plan-to-retry)))))
          ))))

  (testing "does not re-enqueue the job plan when the worker does not allow retries"
    (let [workers {:hard-worker (union-rep/new-worker-definition bluecollar.fake-worker/perform
                                                                 "crunch-numbers"
                                                                 false)}
          _ (union-rep/register-workers workers)
          job-plan (plan/new-job-plan :hard-worker [123])
          _ (plan/on-failure job-plan)]
      (is (nil? (redis/pop-to-processing "crunch-numbers")))
    ))

  (testing "removes the job plan from the failures hash if not retryable"
    (stubbing [plan/retry-on-failure? false]
      (let [job-plan (plan/new-job-plan :hard-worker [123])
            _ (redis/failure-retry-cnt (:uuid job-plan))
            _ (plan/on-failure job-plan)]
        (is (= 0 (redis/failure-retry-cnt (:uuid job-plan)))))))

  (testing "always removes the job plan from the processing queue"
    (let [processing-queue @redis/processing-queue
          job-plan (plan/new-job-plan :hard-worker [1 3])
          _ (redis/push processing-queue (plan/as-json job-plan))
          current-vals (redis/lrange processing-queue 0 0)
          _ (plan/on-failure job-plan)
          remaining-vals (redis/lrange processing-queue 0 0)]
        (is (not (empty? current-vals)))
        (is (empty? remaining-vals))
      ))

  (testing "always increments the total failures counter"
    (let [_ (redis/failure-total-del)
          job-plan (plan/new-job-plan :hard-worker [1 3])
          _ (plan/on-failure job-plan)]
      (is (= 1 (redis/failure-total-cnt))))))

(deftest schedulable-test
  (testing "returns true when a scheduled runtime is present and is in the future"
    (let [a-job-plan (plan/new-job-plan :hard-worker [123] (str (time/plus (time/now) (time/minutes 2))))]
      (is (= true (plan/schedulable? a-job-plan)))))

  (testing "returns false when a scheduled runtime is present but is in the past"
    (let [a-job-plan (plan/new-job-plan :hard-worker [123] (str (time/minus (time/now) (time/minutes 2))))]
      (is (= false (plan/schedulable? a-job-plan)))))

  (testing "returns false when a scheduled runtime is not present"
    (let [a-job-plan (plan/new-job-plan :hard-worker [123])]
      (is (= false (plan/schedulable? a-job-plan))))))

(deftest secs-to-runtime-test
  (testing "returns a positive Long value representing seconds"
    (let [a-job-plan (plan/new-job-plan :hard-worker [123] (str (time/plus (time/now) (time/minutes 2))))]
      (is (= (> 0 (plan/secs-to-runtime a-job-plan))))))

  (testing "returns 0 when the job is not schedulable"
    (let [a-job-plan (plan/new-job-plan :hard-worker [123])]
      (is (= 0 (plan/secs-to-runtime a-job-plan))))))

(deftest hookable-test
  (testing "when JobPlan implements the Hookable protocol"
    (extend-type bluecollar.job_plans.JobPlan
      plan/Hookable
      (before [_] 1)
      (after [_] 2))
    (let [a-job-plan (plan/new-job-plan :hard-worker [123])]
      (is (extends? plan/Hookable bluecollar.job_plans.JobPlan))
      (is (= 1 (plan/before a-job-plan)))
      (is (= 2 (plan/after a-job-plan))))))