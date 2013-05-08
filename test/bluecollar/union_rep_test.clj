(ns bluecollar.union-rep-test
  (:use clojure.test
        bluecollar.test-helper)
  (:require [bluecollar.union-rep :as union-rep]
            [bluecollar.fake-worker]))

(deftest worker-registry-test
  (testing "adds a new worker to the registry"
    (let [hard-worker {:hard-worker {:fn bluecollar.fake-worker/perform
                                     :queue "crunch-numbers"}}
          _ (swap! bluecollar.union-rep/worker-registry conj hard-worker)]
      (is (= (deref bluecollar.union-rep/worker-registry) hard-worker))
      )))

(deftest union-card-check-test
  (testing "if not alreay loaded include the given namespace"
    (let [_ (union-rep/union-card-check 'bluecollar.fake-worker)]
      (is (true? (contains? (loaded-libs) 'bluecollar.fake-worker)))
      (is (true? (eval (list 'bluecollar.fake-worker/perform 1 2)))))
    ))