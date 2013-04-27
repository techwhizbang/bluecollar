(ns bluecollar.redis-message-storage-test
  (:use clojure.test)
  (:require [bluecollar.redis-message-storage :as redis]))

(def redis-test-settings {:host "127.0.0.1", :port 6379, :db 0, :timeout 1})

(use-fixtures :each
  (fn [f]
    (redis/startup redis-test-settings)
    (redis/flushdb)
    (f)))

(deftest push-value-onto-queue
  (testing "pushes a String value onto a named queued"
    (is (= (redis/push "bacon" "eggs") 1)))

  (testing "uses a separate RedisConnection to push a String value"
    (let [redis-conn (redis/->RedisConnection (redis/redis-pool) (redis/redis-settings redis-test-settings))]
      (is (= (redis/push redis-conn "pancakes" "syrup") 1))
      )))

(deftest pop-value-from-queue
  (testing "consumes a value from a named queue"
    (let [_ (redis/push "mocha" "latte")
          value (redis/pop "mocha")]
      (is (= value "latte"))))

  (testing "places the pop valued into the processing queue"
    (let [_ (redis/push "deep dish" "pizza")
          _ (redis/pop "deep dish")
          values (redis/lrange (deref redis/processing-queue) 0 0)]
      (is (= (first values) "pizza")))
    )

  (testing "uses a separate RedisConnection to pop a value"
    (let [redis-conn (redis/->RedisConnection (redis/redis-pool) (redis/redis-settings redis-test-settings))
          _ (redis/push redis-conn "salt" "pepper")]
      (is (= (redis/pop redis-conn "salt") "pepper"))
      )))

(deftest blocking-pop-value-from-queue
  (testing "consumes a value from a named queue"
    (let [_ (redis/push "mocha" "latte")
          value (redis/blocking-pop "mocha")]
      (is (= value "latte")))))

(deftest processing-pop-from-queue
  (testing "it removes the value from the processing queue"
    (let [original-value "latte"
          _ (redis/push "caramel" original-value)
          popped-value (redis/blocking-pop "caramel")
          _ (redis/processing-pop original-value)
          remaining-vals (redis/lrange (deref redis/processing-queue) 0 0)]
      (is (= popped-value "latte"))
      (is (empty? remaining-vals))
      )))