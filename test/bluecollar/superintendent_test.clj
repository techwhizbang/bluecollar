(ns bluecollar.superintendent-test
  (:use clojure.test
        bluecollar.test-helper)
  (:require [bluecollar.redis-message-storage :as redis]
            [bluecollar.superintendent :as bossman]
            [bluecollar.job-plans :as plan]
            [cheshire.core :as json]))

(use-redis-test-setup)

;(deftest event-processor-listen-test
;  (testing "continues to listen to events and dispatch workers"
;    (let [_ (future (processor/start testing-queue-name))
;          json (json/generate-string {:ns "bluecollar.worker-test" :args [3 2]})
;          _ (redis/push testing-queue-name json)
;          _ (Thread/sleep 1000)
;          _ (processor/stop)]
;      )
;    ))

(deftest event-processor-worker-dispatch-test
  (testing "the perform method is called"
    (let [_ (future (bossman/start testing-queue-name))
          plan-as-json (plan/as-json 'bluecollar.fake-worker [3 2])
          _ (redis/push testing-queue-name plan-as-json)
          _ (Thread/sleep 1000)
          _ (bossman/stop)]
      )
    ))