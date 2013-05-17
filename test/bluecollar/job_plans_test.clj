(ns bluecollar.job-plans-test
  (:use clojure.test
    bluecollar.test-helper)
  (:require [clj-time.core :as time]
            [bluecollar.job-plans :as plan]
            [bluecollar.redis-message-storage :as redis]
            [bluecollar.union-rep :as union-rep]
            [bluecollar.fake-worker]))

(use-fixtures :each (fn [f]
  (redis/startup redis-test-settings)
  (redis/flushdb)
  (reset! bluecollar.fake-worker/perform-called false)
  (f)))

(deftest job-plan-struct-test
  (testing "takes a worker and a vector of args"
    (let [job-plan (struct plan/job-plan :a-worker ["a" "b"])]
      (is (= job-plan {:worker :a-worker, 
                       :args ["a" "b"], 
                       :uuid nil
                       :scheduled-runtime nil}))))

  (testing "takes a worker, vector of args, and a UUID"
    (let [uuid (str (java.util.UUID/randomUUID))
          job-plan (struct plan/job-plan :b-worker ["c" "d"] uuid)]
        (is (= job-plan {:worker :b-worker, 
                         :args ["c" "d"], 
                         :uuid uuid
                         :scheduled-runtime nil}))))

  (testing "takes a worker, vector of args, UUID, and scheduled-runtime"
    (let [uuid (str (java.util.UUID/randomUUID))
          now (time/now)
          job-plan (struct plan/job-plan :b-worker ["c" "d"] uuid now)]
        (is (= job-plan {:worker :b-worker, 
                         :args ["c" "d"], 
                         :uuid uuid
                         :scheduled-runtime now}))))
  )

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
        "{\"worker\":\"hard-worker\",\"args\":[1,2],\"uuid\":null,\"scheduled-runtime\":null}")))
    )

  (testing "converts a plan to JSON with a scheduled-runtime"
    (let [now (time/now)
          job-plan (plan/new-job-plan :hard-worker [1 2] nil now)]
      (is (= (plan/as-json job-plan)
        (str "{\"worker\":\"hard-worker\",\"args\":[1,2],\"uuid\":null,\"scheduled-runtime\":\"" now "\"}")))))
  )

(deftest plan-from-json-test
  (testing "converts a plan in JSON to a map"
    (let [a-job-plan (plan/new-job-plan :hard-worker [1 2])
          a-json-plan (plan/as-json a-job-plan)]
      (is (= (plan/from-json a-json-plan) a-job-plan))
      )))

(deftest plan-for-worker-test
  (testing "converts a plan map for a worker"
    (let [job-plan (plan/new-job-plan :hard-worker [1 2])]
      (is (fn? (plan/for-worker job-plan)))
      ))

  (testing "makes an executable function for the worker"
    (let [workers {:hard-worker (struct union-rep/worker-definition 
                                        bluecollar.fake-worker/perform
                                        "crunch-numbers"
                                        false)}
          _ (union-rep/register-workers workers)
          a-job-plan (plan/new-job-plan :hard-worker [1 2])
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
          _ (plan/enqueue :hard-worker [1 3])
          job-plan (plan/from-json (redis/pop "crunch-numbers"))]
      (is (= :hard-worker (get job-plan :worker)))
      (is (= [1 3] (get job-plan :args)))
      (is (not (nil? (re-find uuid-regex (get job-plan :uuid)))))
      ))
  
  (testing "throws a RuntimeException when an unregistered worker is encountered"
    (let [_ (reset! bluecollar.union-rep/registered-workers {})]
      (is (thrown-with-msg? RuntimeException #":hard-worker was not found in the worker registry." (plan/enqueue :hard-worker [1 3])))
      )
    ))

(deftest on-success-test
  (testing "successfully removes a job plan from the processing queue"
    (let [processing-queue (deref redis/processing-queue)
          job-plan (plan/new-job-plan :hard-worker [1 3])
          _ (redis/push processing-queue (plan/as-json job-plan))
          current-vals (redis/lrange processing-queue 0 0)
          _ (plan/on-success job-plan)
          remaining-vals (redis/lrange processing-queue 0 0)]
        (is (not (empty? current-vals)))
        (is (empty? remaining-vals))
      )
    ))
