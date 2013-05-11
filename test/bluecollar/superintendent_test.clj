(ns bluecollar.superintendent-test
  (:use clojure.test
    bluecollar.test-helper)
  (:require [bluecollar.redis-message-storage :as redis]
    [bluecollar.fake-worker]
    [bluecollar.union-rep :as union-rep]
    [bluecollar.superintendent :as boss]
    [bluecollar.job-plans :as plan]
    [cheshire.core :as json]))

(use-redis-test-setup)

(use-fixtures :each (fn [f]
  (reset! bluecollar.fake-worker/perform-called false)
  (f)))

(deftest superintendent-end-to-end-test
  (testing "that the message is passed to the foreman and the foreman dispatches work"
    (let [workers {:hard-worker (struct union-rep/worker-definition 
                                        bluecollar.fake-worker/perform
                                        testing-queue-name
                                        false)}
          _ (union-rep/register-workers workers)
          _ (future (boss/start testing-queue-name 5))
          _ (plan/enqueue :hard-worker [3 2])
          _ (Thread/sleep 2000)
          _ (boss/stop)
          in-processing-vals (redis/lrange (deref redis/processing-queue) 0 0)]
      (is (true? (deref bluecollar.fake-worker/perform-called)))
      (is (empty? in-processing-vals))
      )
    ))