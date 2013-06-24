(ns bluecollar.client-test
  (:use clojure.test
        bluecollar.test-helper
        bluecollar.client)
  (:require [bluecollar.union-rep :as union-rep]
            [bluecollar.job-plans :as plan]
            [bluecollar.fake-worker]
            [bluecollar.redis :as redis]
            [clj-time.core :as time]))

(use-fixtures :each (fn [f]
  (redis/startup redis-test-settings)
  (redis/flushdb)
  (reset! bluecollar.fake-worker/perform-called false)
  (union-rep/register-workers {:hard-worker (struct union-rep/worker-definition 
                                                    bluecollar.fake-worker/perform
                                                    "crunch-numbers"
                                                    false)})
  (f)))

(deftest async-job-for-test
  (testing "successfully sends a job for a registered worker to process"
    (is (nil? (redis/pop "crunch-numbers")))
    (is (not (nil? (re-find uuid-regex (async-job-for :hard-worker [1 3])))))
    (is (not (nil? (redis/pop "crunch-numbers")))))

  (testing "successfully sends a job with a scheduled runtime"
    (is (nil? (redis/pop "crunch-numbers")))
    (is (not (nil? (re-find uuid-regex (async-job-for :hard-worker [1 3] (str (time/now)))))))
    (is (not (nil? (redis/pop "crunch-numbers")))))
  
  (testing "throws a RuntimeException when an unregistered worker is encountered"
    (let [_ (reset! bluecollar.union-rep/registered-workers {})]
      (is (thrown-with-msg? RuntimeException #":hard-worker was not found in the worker registry." (async-job-for :hard-worker [1 3])))
      )
    ))