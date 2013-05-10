(ns bluecollar.union-rep-test
  (:use clojure.test
    bluecollar.test-helper)
  (:require [bluecollar.union-rep :as union-rep]
    [bluecollar.fake-worker]))

(use-fixtures :each (fn [f]
  (reset! union-rep/registered-workers {})
  (f)))

(deftest registered-workers-test
  (testing "registers a single new worker-definition"
    (let [hard-worker (struct union-rep/worker-definition bluecollar.fake-worker/perform "crunch-numbers" false)
          _ (union-rep/register-worker :hard-worker hard-worker)]
      (is (= (deref union-rep/registered-workers) {:hard-worker hard-worker}))
      ))
  (testing "registers all of the worker-definitions"
    (let [hard-worker-1 (struct union-rep/worker-definition bluecollar.fake-worker/perform "crunch-numbers" false)
          hard-worker-2 (struct union-rep/worker-definition bluecollar.fake-worker/perform "crunch-numbers" false)
          all-workers-defs {:one hard-worker-1 :two hard-worker-2}
          _ (union-rep/register-workers all-workers-defs)]
      (is (= (deref union-rep/registered-workers) all-workers-defs))
      )))