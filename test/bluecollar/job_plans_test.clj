(ns bluecollar.job-plans-test
  (:use clojure.test)
  (:require [bluecollar.job-plans :as plan]))

(deftest plan-as-map-test
  (testing "converts a ns and arguments into a job plan map"
    (is (= (plan/as-map 'bluecollar.job-plans [1 2 3])
      {"ns" 'bluecollar.job-plans/perform, "args" [1 2 3]}))
    ))

(deftest plan-as-json-test
  (testing "converts a plan map to JSON"
    (is (= (plan/as-json 'bluecollar.job-plans [1 2 3])
      "{\"ns\":\"bluecollar.job-plans/perform\",\"args\":[1,2,3]}"))))

(deftest plan-from-json-test
  (testing "converts a plan in JSON to a map"
    (let [a-map-plan {"ns" 'bluecollar.job-plans/perform, "args" [1 2 3]}
          a-json-plan (plan/as-json 'bluecollar.job-plans [1 2 3])]
      (is (= (plan/from-json a-json-plan) a-map-plan))
      )))

(deftest plan-as-list-test
  (testing "converts a plan map to a list"
    (let [plan-map (plan/as-map 'bluecollar.job-plans [1 2 3])
          plan-as-list (plan/as-list plan-map)]
      (is (= plan-as-list '(bluecollar.job-plans/perform [1 2 3])))
      )))

(deftest plan-for-worker-test
  (testing "converts a plan map for a worker"
    (let [plan-map {"ns" 'bluecollar.job-plans/perform, "args" [1 2 3]}]
      (is (fn? (plan/for-worker plan-map)))
      )))
