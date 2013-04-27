(ns bluecollar.foreman-test
  (:use clojure.test)
  (:require [bluecollar.foreman :as foreman]))

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
  (testing "can dispatch a worker"
    (let [counter (atom 0)]
      (do
        (foreman/start-workers number-of-workers)
        (foreman/dispatch-worker (fn [] (swap! counter inc)))
        (Thread/sleep 500)
        (is (= @counter 1)))
      )))
