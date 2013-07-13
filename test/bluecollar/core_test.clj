(ns bluecollar.core-test
  (:use clojure.test
        bluecollar.client
        bluecollar.core
        bluecollar.test-helper)
  (:require [bluecollar.fake-worker :as fake-worker]
            [bluecollar.job-plans :as plan]
            [bluecollar.union-rep :as union-rep]
            [bluecollar.redis :as redis]))

(def queue-specs {"high-importance" 10 "medium-importance" 5 "low-importance" 5})
(def worker-specs {:worker-one {:fn bluecollar.fake-worker/counting, :queue "high-importance", :retry false}
                   :worker-two {:fn bluecollar.fake-worker/explode, :queue "low-importance", :retry true}} )

(use-fixtures :each (fn [f]
  (reset! bluecollar.fake-worker/fake-worker-failures 0)
  (reset! bluecollar.fake-worker/cnt-me 0)
  (union-rep/clear-registered-workers)
  (redis/flushdb)
  (f)
  (reset! bluecollar.fake-worker/fake-worker-failures 0)
  (reset! bluecollar.fake-worker/cnt-me 0)))

(deftest processing-queue-recovery-test
  (testing "recovers jobs uncompleted in the processing queue"
    (union-rep/register-worker :hard-worker (union-rep/new-worker-definition bluecollar.fake-worker/perform "crunch-numbers" false))
    (let [job-plan (plan/new-job-plan :hard-worker [])
          job-json (plan/as-json job-plan)
          _ (redis/push "crunch-numbers" job-json)
          _ (redis/blocking-pop "crunch-numbers")
          queue-cnt (count (redis/lrange "crunch-numbers" 0 0))
          processing-cnt (count (redis/lrange @redis/processing-queue 0 0))]
          (is (= 0 queue-cnt))
          (is (= 1 processing-cnt))
          (processing-queue-recovery)
          (is (= 1 (count (redis/lrange "crunch-numbers" 0 0))))
          (is (= 0 (count (redis/lrange @redis/processing-queue 0 0))))
          (is (= job-json (redis/blocking-pop "crunch-numbers"))))))

(deftest bluecollar-setup-teardown-test
  (testing "can successfully setup and teardown the bluecollar environment"
    (bluecollar-setup queue-specs worker-specs)
    (Thread/sleep 1000)
    ; check that there are the correct number of JobSites
    (is (= (count (keys queue-specs)) (count @job-sites)))
    ; send some work that should be processed on successful startup
    (async-job-for :worker-one [])
    (async-job-for :worker-two [])
    (Thread/sleep 1000)
    ; check that the work was processed
    (is (= 1 @fake-worker/cnt-me))
    (is (= 1 @fake-worker/fake-worker-failures))
    ; tear it down
    (bluecollar-teardown)
    (Thread/sleep 1000)
    ; ensure the JobSites are empty
    (is (empty? @job-sites))
    ; send more work but it won't get processed
    (is (thrown-with-msg? RuntimeException #":worker-one was not found in the worker registry." (async-job-for :worker-one [])))
    (is (thrown-with-msg? RuntimeException #":worker-two was not found in the worker registry." (async-job-for :worker-two [])))
    
    ))