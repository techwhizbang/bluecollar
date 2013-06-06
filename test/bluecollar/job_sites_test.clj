(ns bluecollar.job-sites-test
  (:use clojure.test
    bluecollar.test-helper)
  (:require [bluecollar.redis :as redis]
            [bluecollar.fake-worker]
            [bluecollar.union-rep :as union-rep]
            [bluecollar.job-sites :as job-site]
            [bluecollar.job-plans :as plan]
            [cheshire.core :as json]))

(use-fixtures :each (fn [f]
  (redis/startup redis-test-settings)
  (redis/flushdb)
  (reset! bluecollar.fake-worker/perform-called false)
  (f)))

(deftest job-site-end-to-end-test
  (testing "that the job plan is passed to the foreman and the foreman dispatches work"
    (let [workers {:hard-worker (struct union-rep/worker-definition 
                                        bluecollar.fake-worker/perform
                                        testing-queue-name
                                        false)}
          _ (union-rep/register-workers workers)
          _ (future (job-site/start testing-queue-name 5))
          _ (plan/enqueue :hard-worker [3 2])
          _ (Thread/sleep 2000)
          _ (job-site/stop)
          in-processing-vals (redis/lrange (deref redis/processing-queue) 0 0)]
      (is (true? (deref bluecollar.fake-worker/perform-called)))
      (is (empty? in-processing-vals))
      ))

  (testing "a failing worker is retried the maximum number of times without crashing the process"
    (let [_ (reset! plan/delay-base 1)
          workers {:failing-worker (struct union-rep/worker-definition 
                                           bluecollar.fake-worker/explode
                                           testing-queue-name
                                           true)}
          _ (union-rep/register-workers workers)
          _ (future (job-site/start testing-queue-name 5))
          _ (plan/enqueue :failing-worker [])
          _ (Thread/sleep 2000)
          _ (job-site/stop)]
      (is (= 25 (deref bluecollar.fake-worker/fake-worker-failures)))
      )))