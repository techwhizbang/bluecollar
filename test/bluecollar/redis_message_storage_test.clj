(ns bluecollar.redis-message-storage-test
  (:use clojure.test
        bluecollar.test-helper)
  (:require [bluecollar.redis-message-storage :as redis]))

(use-redis-test-setup)

(deftest push-value-onto-queue
  (testing "pushes a String value onto a named queued"
    (is (= (redis/push "bacon" "eggs") 1)))

  (testing "uses a separate RedisConnection to push a String value"
    (let [redis-conn (redis/new-connection redis-test-settings)]
      (is (= (redis/push "pancakes" "syrup" redis-conn) 1))
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
    (let [redis-conn (redis/new-connection redis-test-settings)
          _ (redis/push "salt" "pepper" redis-conn)]
      (is (= (redis/pop "salt" redis-conn) "pepper"))
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