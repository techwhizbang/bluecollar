(ns bluecollar.foreman-test
  (:use clojure.test
        bluecollar.test-helper)
  (:require [bluecollar.foreman :as foreman]
    [bluecollar.job-plans :as plan]
    [bluecollar.fake-worker]
    [bluecollar.union-rep :as union-rep]))

(def number-of-workers 5)

(use-redis-test-setup)

(use-fixtures :each (fn [f]
  (reset! bluecollar.fake-worker/perform-called false)
  (f)))

(deftest foreman-start-stop-workers-test
  (testing "all of the workers can start and stop"
    (do
      (foreman/start-workers number-of-workers)
      (is (= (foreman/worker-count) number-of-workers))
      (foreman/stop-workers))
    (Thread/sleep 1000)
    (is (= (foreman/worker-count) 0))
    ))

(deftest foreman-dispatch-worker-test
  (testing "can dispatch a generic worker"
    (let [counter (atom 0)]
      (do
        (foreman/start-workers number-of-workers)
        (foreman/dispatch-worker (fn [] (swap! counter inc)))
        (Thread/sleep 500)
        (is (= @counter 1)))
      ))

  (testing "can dispatch a worker based on a job plan"
    (let [hard-worker {:fake-worker {:fn bluecollar.fake-worker/perform
                                     :queue testing-queue-name}}
          _ (swap! bluecollar.union-rep/worker-registry conj hard-worker)
          a-job-plan (struct plan/job-plan :fake-worker [1 2])
          job-for-worker (plan/for-worker a-job-plan)]
      (do
        (foreman/start-workers number-of-workers)
        (foreman/dispatch-worker job-for-worker)
        (Thread/sleep 500)
        (is (true? (deref bluecollar.fake-worker/perform-called)))
        )
      )))

