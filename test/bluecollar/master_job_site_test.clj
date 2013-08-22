(ns bluecollar.master-job-site-test
  (:use clojure.test
        bluecollar.test-helper
        bluecollar.lifecycle
        bluecollar.client
        bluecollar.master-job-site)
  (:require [bluecollar.workers-union :as workers-union]
            [bluecollar.redis :as redis]
            [bluecollar.job-plans :as plan]
            [bluecollar.fake-worker]
            [bluecollar.keys-and-queues :as keys-qs]))

(use-fixtures :each (fn [f]
  (workers-union/clear-registered-workers)
  (redis/startup redis-test-settings)
  (redis/flushdb)
  (reset! bluecollar.fake-worker/perform-called false)
  (reset! bluecollar.fake-worker/fake-worker-failures 0)
  (f)))

(deftest master-job-site-queue-test
  (testing "pops a job plan from the master queue, places it into intended queue, clears master processing queue"
    (let [workers {:hard-worker (workers-union/new-unionized-worker bluecollar.fake-worker/perform
                                                                    "intended-queue"
                                                                    false)}
          master-job-site (new-master-job-site)]
      (keys-qs/register-queues ["intended-queue"] nil)
      (keys-qs/register-keys)
      (workers-union/register-workers workers)
      (let [uuid (async-job-for :hard-worker [{"mastersite" "test"} 2])]      
        (startup master-job-site)
        (Thread/sleep 2000)
        (is (= uuid (:uuid (plan/from-json (redis/rpop "intended-queue")))))
        (is (nil? (redis/rpop keys-qs/master-processing-queue-name))))
      (shutdown master-job-site)
      )))