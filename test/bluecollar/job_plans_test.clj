(ns bluecollar.job-plans-test
  (:use clojure.test)
  (:require [bluecollar.job-plans :as plan]
            [bluecollar.fake-worker]))

(deftest plan-as-map-test
  (testing "converts a ns and arguments into a job plan map"
    (is (= (plan/as-map 'bluecollar.fake-worker [1 2])
      {"ns" 'bluecollar.fake-worker, "args" [1 2]}))
    ))

(deftest plan-as-json-test
  (testing "converts a plan map to JSON"
    (is (= (plan/as-json 'bluecollar.fake-worker [1 2])
      "{\"ns\":\"bluecollar.fake-worker\",\"args\":[1,2]}"))))

(deftest plan-from-json-test
  (testing "converts a plan in JSON to a map"
    (let [a-map-plan {"ns" 'bluecollar.fake-worker, "args" [1 2]}
          a-json-plan (plan/as-json 'bluecollar.fake-worker [1 2])]
      (is (= (plan/from-json a-json-plan) a-map-plan))
      )))

(deftest plan-as-list-test
  (testing "converts a plan map to a list"
    (let [plan-map (plan/as-map 'bluecollar.fake-worker [1 2])
          plan-as-list (plan/as-list plan-map)]
      (is (= plan-as-list '(bluecollar.fake-worker 1 2)))
      )))

(deftest plan-for-worker-test
  (testing "converts a plan map for a worker"
    (let [plan-map {"ns" 'bluecollar.fake-worker, "args" [1 2]}]
      (is (fn? (plan/for-worker plan-map)))
      ))

  (testing "makes an executable function for the worker"
    (let [plan-map {"ns" 'bluecollar.fake-worker, "args" [1 2]}
          _ ((plan/for-worker plan-map))]
      (is (true? (deref bluecollar.fake-worker/perform-called)))
      ))
  )
