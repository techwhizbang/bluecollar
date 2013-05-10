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
      {"worker" :hard-worker, "args" [1 2]}))
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
    (let [hard-worker {:hard-worker {:fn bluecollar.fake-worker/perform
                                     :queue "crunch-numbers"}}
          _ (swap! bluecollar.union-rep/registered-workers conj hard-worker)
          plan-map {"worker" :hard-worker, "args" [1 2]}
          _ ((plan/for-worker plan-map))]
      (is (true? (deref bluecollar.fake-worker/perform-called)))
      ))
  )

(deftest enqueue-test
  (testing "successfully enqueues a job plan for a registered worker"
    (let [hard-worker {:hard-worker {:fn bluecollar.fake-worker/perform
                                     :queue testing-queue-name}}
          _ (swap! bluecollar.union-rep/registered-workers conj hard-worker)
          _ (plan/enqueue :hard-worker [1 3])]
      (is (= (redis/pop testing-queue-name) "{\"worker\":\"hard-worker\",\"args\":[1,3]}"))
      ))
  
  (testing "throws a RuntimeException when an unregistered worker is encountered"
    (let [_ (reset! bluecollar.union-rep/registered-workers {})]
      (is (thrown-with-msg? RuntimeException #":hard-worker was not found in the worker registry." (plan/enqueue :hard-worker [1 3])))
      )
    ))
