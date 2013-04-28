(ns bluecollar.event-processor-test
  (:use clojure.test)
  (:require [bluecollar.redis-message-storage :as redis]
            [bluecollar.event-processor :as processor]))

(def redis-test-settings {:host "127.0.0.1", :port 6379, :db 0, :timeout 1})
(def testing-queue-name "testing-queue-name")


(use-fixtures :each
  (fn [f]
    (redis/flushdb)
    (redis/startup redis-test-settings)
    (f)))


(deftest event-processor-listen-test
  (testing "continues to listen to events and dispatch workers"
    (let [_ (future (processor/start testing-queue-name))
          _ (redis/push testing-queue-name "hey hey hey")
          _ (Thread/sleep 500)
          _ (processor/stop)]
      )
    ))