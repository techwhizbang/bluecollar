(ns bluecollar.redis-message-storage-test
  (:use clojure.test
  			bluecollar.redis-message-storage))

(def redis-test-settings {:host "127.0.0.1", :port 6379, :db 0, :timeout 1})

(use-fixtures :each 
  (fn [f] 
    (startup redis-test-settings)
    (flushdb)
    (f)))

(deftest push-value-onto-queue
  (testing "pushes a String value onto a named queued"
    (is (= (push "bacon" "eggs") 1)))

  (testing "uses a separate RedisConnection to push a String value"
    (let [redis-conn (->RedisConnection (redis-pool) (redis-settings redis-test-settings))]
      (is (= (push redis-conn "pancakes" "syrup") 1))
      )))

(deftest consume-value-from-queue
  (testing "consumes a value from a named queue"
    (let [_ (push "mocha" "latte")
          value (consume "mocha")]
          (is (= value "latte"))))

  (testing "uses a separate RedisConnection to consume a value"
    (let [redis-conn (->RedisConnection (redis-pool) (redis-settings redis-test-settings))
          _ (push redis-conn "salt" "pepper")]
      (is (= (consume redis-conn "salt") "pepper"))
      )))