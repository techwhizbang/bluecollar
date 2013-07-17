(ns bluecollar.workers-union-test
  (:use clojure.test
    bluecollar.test-helper)
  (:require [bluecollar.workers-union :as workers-union]
            [bluecollar.fake-worker]))

(use-fixtures :each (fn [f]
  (workers-union/clear-registered-workers)
  (f)))

(deftest registered-workers-test
  (testing "registers a single new worker-definition"
    (let [hard-worker (workers-union/new-unionized-worker bluecollar.fake-worker/perform "crunch-numbers" false)
          _ (workers-union/register-worker :hard-worker hard-worker)]
      (is (= (deref workers-union/registered-workers) {:hard-worker hard-worker}))
      ))
  (testing "registers all of the worker-definitions"
    (let [hard-worker-1 (workers-union/new-unionized-worker bluecollar.fake-worker/perform "crunch-numbers" false)
          hard-worker-2 (workers-union/new-unionized-worker bluecollar.fake-worker/perform "crunch-numbers" false)
          all-workers-defs {:one hard-worker-1 :two hard-worker-2}
          _ (workers-union/register-workers all-workers-defs)]
      (is (= (deref workers-union/registered-workers) all-workers-defs))
      )))

(deftest find-worker-test
  (testing "returns a unionized worker that has been registered"
    (let [hard-worker (workers-union/new-unionized-worker bluecollar.fake-worker/perform "crunch-numbers" false)
          _ (workers-union/register-worker :hard-worker hard-worker)]
      (is (= (workers-union/find-worker :hard-worker) hard-worker))
      ))
  (testing "returns nil if a unionized worker is not found"
    (is (nil? (workers-union/find-worker :missing-worker)))
      ))