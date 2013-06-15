(ns bluecollar.core-test
  (:use clojure.test
        bluecollar.core
        bluecollar.test-helper)
  (:require [bluecollar.fake-worker :as fake-worker]
            [bluecollar.job-plans :as plan]
            [bluecollar.redis :as redis]))

(def queue-specs {:high-importance 10 :medium-importance 5 :low-importance 5})
(def worker-specs {:worker-one {:fn bluecollar.fake-worker/counting, :queue :high-importance, :retry false}
                   :worker-two {:fn bluecollar.fake-worker/explode, :queue :low-importance, :retry true}} )

(use-fixtures :each (fn [f]
  (redis/startup redis-test-settings)
  (redis/flushdb)
  (reset! bluecollar.fake-worker/fake-worker-failures 0)
  (reset! bluecollar.fake-worker/cnt-me 0)
  (f)
  (reset! bluecollar.fake-worker/fake-worker-failures 0)
  (reset! bluecollar.fake-worker/cnt-me 0)))

(deftest bluecollar-startup-shutdown-test
  (testing "can successfully startup and shutdown the bluecollar environment"
    (bluecollar-startup queue-specs worker-specs)
    (Thread/sleep 1000)
    ; send some work that should be processed on successful startup
    (plan/enqueue :worker-one [])
    (plan/enqueue :worker-two [])
    (Thread/sleep 1000)
    ; check that the work was processed
    (is (= 1 @fake-worker/cnt-me))
    (is (= 1 @fake-worker/fake-worker-failures))
    ; shut it down
    (bluecollar-shutdown)
    (Thread/sleep 1000)
    ; send more work but it won't get processed
    (plan/enqueue :worker-one [])
    (plan/enqueue :worker-two [])
    ; ensure that no more work was processed
    (is (= 1 @fake-worker/cnt-me))
    (is (= 1 @fake-worker/fake-worker-failures))
    ))
