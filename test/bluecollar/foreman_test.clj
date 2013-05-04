(ns bluecollar.foreman-test
  (:use clojure.test)
  (:require [bluecollar.foreman :as foreman]
    [bluecollar.job-plans :as plan]
    [bluecollar.fake-worker]
    [bluecollar.labor-union-rep :as labor-rep]))

(def number-of-workers 5)

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
    (let [_ (labor-rep/union-card-check 'bluecollar.fake-worker)
          job-map (plan/as-map 'bluecollar.fake-worker [1 2])
          job-for-worker (plan/for-worker job-map)]
      (do
        (foreman/start-workers number-of-workers)
        (foreman/dispatch-worker job-for-worker)
        (Thread/sleep 500)
        (is (true? (deref bluecollar.fake-worker/perform-called)))
        )
      )))

