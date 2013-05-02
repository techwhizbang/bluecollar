(ns bluecollar.superintendent-test
  (:use clojure.test
        bluecollar.test-helper)
  (:require [bluecollar.redis-message-storage :as redis]
            [bluecollar.fake-worker]
            [bluecollar.superintendent :as bossman]
            [bluecollar.job-plans :as plan]
            [cheshire.core :as json]))

(use-redis-test-setup)

(use-fixtures :each (fn [f]
  (reset! bluecollar.fake-worker/perform-called false)
  (f)))


(deftest superintendent-end-to-end-test
  (testing "that the message is passed to the foreman and the foreman dispatches work"
    (let [_ (future (bossman/start testing-queue-name 5))
          plan-as-json (plan/as-json 'bluecollar.fake-worker [3 2])
          _ (redis/push testing-queue-name plan-as-json)
          _ (Thread/sleep 2000)
          _ (bossman/stop)]
      (is (true? (deref bluecollar.fake-worker/perform-called))))
    ))