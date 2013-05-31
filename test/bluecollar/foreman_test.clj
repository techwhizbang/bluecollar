(ns bluecollar.foreman-test
  (:use clojure.test
        bluecollar.test-helper)
  (:require [bluecollar.foreman :as foreman]
            [bluecollar.job-plans :as plan]
            [bluecollar.fake-worker]
            [clj-time.core :as time]
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
    (let [workers {:fake-worker (struct union-rep/worker-definition
                                        bluecollar.fake-worker/perform
                                        testing-queue-name false)}
          _ (union-rep/register-workers workers)
          a-job-plan (plan/new-job-plan :fake-worker [1 2])
          job-for-worker (plan/for-worker a-job-plan)]
      (do
        (foreman/start-workers number-of-workers)
        (foreman/dispatch-worker job-for-worker)
        (Thread/sleep 500)
        (is (true? (deref bluecollar.fake-worker/perform-called))))
      )))

(deftest foreman-dispatch-scheduled-worker-test
  (testing "can dispatch a worker based on a scheduled job plan"
    (let [workers {:fake-worker (struct union-rep/worker-definition
                                        bluecollar.fake-worker/perform
                                        testing-queue-name false)}
          _ (union-rep/register-workers workers)
          a-job-plan (plan/new-job-plan :fake-worker [1 2] (str (time/plus (time/now) (time/secs 2))))]
      (do
        (foreman/start-workers number-of-workers)
        (foreman/dispatch-scheduled-worker a-job-plan)
        (Thread/sleep 3000)
        (is (true? (deref bluecollar.fake-worker/perform-called))))
      )))

