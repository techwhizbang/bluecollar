(ns bluecollar.job-plans-test
  (:use clojure.test
    bluecollar.test-helper)
  (:require [bluecollar.job-plans :as plan]
    [bluecollar.redis-message-storage :as redis]
    [bluecollar.union-rep :as union-rep]
    [bluecollar.fake-worker]))

(use-redis-test-setup)

(use-fixtures :each (fn [f]
  (reset! bluecollar.fake-worker/perform-called false)
  (f)))

(deftest plan-as-struct-test
  (testing "converts a ns and arguments into a job plan map"
    (is (= (struct plan/job-plan :hard-worker [1 2])
      {:worker :hard-worker, :args [1 2]}))
    ))

(deftest plan-as-json-test
  (testing "converts a plan map to JSON"
    (is (= (plan/as-json :hard-worker [1 2])
      "{\"worker\":\"hard-worker\",\"args\":[1,2]}"))))

(deftest plan-from-json-test
  (testing "converts a plan in JSON to a map"
    (let [a-job-plan (struct plan/job-plan :hard-worker [1 2])
          a-json-plan (plan/as-json :hard-worker [1 2])]
      (is (= (plan/from-json a-json-plan) a-job-plan))
      )))

(deftest plan-for-worker-test
  (testing "converts a plan map for a worker"
    (let [job-plan (struct plan/job-plan :hard-worker [1 2])]
      (is (fn? (plan/for-worker job-plan)))
      ))

  (testing "makes an executable function for the worker"
    (let [workers {:hard-worker (struct union-rep/worker-definition 
                                        bluecollar.fake-worker/perform
                                        "crunch-numbers"
                                        false)}
          _ (union-rep/register-workers workers)
          a-job-plan (struct plan/job-plan :hard-worker [1 2])
          _ ((plan/for-worker a-job-plan))]
      (is (true? (deref bluecollar.fake-worker/perform-called)))
      ))
  )

(deftest enqueue-test
  (testing "successfully enqueues a job plan for a registered worker"
    (let [workers {:hard-worker (struct union-rep/worker-definition 
                                        bluecollar.fake-worker/perform
                                        "crunch-numbers"
                                        false)}
          _ (union-rep/register-workers workers)
          _ (plan/enqueue :hard-worker [1 3])]
      (is (= (redis/pop "crunch-numbers") "{\"worker\":\"hard-worker\",\"args\":[1,3]}"))
      ))
  
  (testing "throws a RuntimeException when an unregistered worker is encountered"
    (let [_ (reset! bluecollar.union-rep/registered-workers {})]
      (is (thrown-with-msg? RuntimeException #":hard-worker was not found in the worker registry." (plan/enqueue :hard-worker [1 3])))
      )
    ))

(deftest on-success-test
  (testing "successfully removes a job plan from the processing queue"
    (let [workers {:hard-worker (struct union-rep/worker-definition 
                                        bluecollar.fake-worker/perform
                                        "crunch-numbers"
                                        false)}
          _ (union-rep/register-workers workers)
          job-plan (struct plan/job-plan :hard-worker [1 3])
          _ (plan/enqueue job-plan)
          current-vals (redis/lrange (deref redis/processing-queue) 0 0)
          _ (plan/on-success job-plan)
          remaining-vals (redis/lrange (deref redis/processing-queue) 0 0)]
        (is (not (empty? current-vals)))
        (is (empty? remaining-vals))
      )
    ))
