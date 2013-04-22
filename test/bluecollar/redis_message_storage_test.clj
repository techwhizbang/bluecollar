(ns bluecollar.redis-message-storage-test
  (:use clojure.test
  			bluecollar.message-storage
  			bluecollar.redis-message-storage
  			))
(def redis-test-settings {:host "127.0.0.1", :port 6379, :db 0, :timeout 1})
(def redis-msg-store (->RedisMessageStorage))
(def redis-test-pool (connection-pool redis-msg-store redis-test-settings))

(use-fixtures :each 
  (fn [f] 
    redis-test-pool
    (flushdb redis-msg-store)
    (f)))

(deftest push-value-onto-queue
  (testing "pushes a String value onto a named queued"
    (is (= (push redis-msg-store "bacon" "eggs") 1))))

(deftest consume-value-from-queue
  (testing "consumes a value from a named queue"
    (let [_ (push redis-msg-store "mocha" "latte")
          value (consume redis-msg-store "mocha")]
          (is (= value "latte")))))