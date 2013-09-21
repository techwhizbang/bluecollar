(ns bluecollar.redis-test
  (:use clojure.test
        bluecollar.test-helper)
  (:require [bluecollar.redis :as redis]
            [bluecollar.keys-and-queues :as keys-qs]))

(use-fixtures :each (fn [f]
  (redis-setup)
  (keys-qs/register-keys)
  (keys-qs/register-queues nil)
  (f)))

(deftest failure-retry-cnt-test
  (testing "returns zero when there are no failures"
    (is (= 0 (redis/failure-retry-cnt "no-failures"))))

  (testing "increments an entry that does not exist yet by one"
    (redis/failure-retry-inc "burger")
    (is (= 1 (redis/failure-retry-cnt "burger"))))

  (testing "increments an entry that already exists by one"
    (redis/failure-retry-inc "cheese")
    (redis/failure-retry-inc "cheese")
    (is (= 2 (redis/failure-retry-cnt "cheese")))))

(deftest failure-retry-del-test
  (testing "it removes a key from the failures hash"
    (redis/failure-retry-inc "pizza")
    (is (= 1 (redis/failure-retry-cnt "pizza")))
    (redis/failure-retry-del "pizza")
    (is (= 0 (redis/failure-retry-cnt "pizza"))))

  (testing "it doesn't complain if the key doesn't exist in the failures hash"
    (redis/failure-retry-del "mushroom frittata")
    (is (= 0 (redis/failure-retry-cnt "mushroom frittata")))))

(deftest failure-total-test
  (testing "returns zero when there are no failures"
    (is (= 0 (redis/failure-total-cnt))))
  
  (testing "increments the count by one"
    (dorun (repeatedly 2 #(redis/failure-total-inc)))
    (is (= 2 (redis/failure-total-cnt)))))

(deftest failure-total-del-test
  (redis/failure-total-inc)
  (is (= 1 (redis/failure-total-cnt)))
  (redis/failure-total-del)
  (is (= 0 (redis/failure-total-cnt))))

(deftest success-total-test
  (testing "returns zero when there are no successes"
    (is (= 0 (redis/success-total-cnt))))

  (testing "increments the count by one"
    (dorun (repeatedly 2 #(redis/success-total-inc)))
    (is (= 2 (redis/success-total-cnt)))))

(deftest success-total-del-test
  (redis/success-total-inc)
  (is (= 1 (redis/success-total-cnt)))
  (redis/success-total-del)
  (is (= 0 (redis/success-total-cnt))))

(deftest get-set-key-test
  (redis/setex "belgian" "tripel" 1)
  (is (= "tripel" (redis/get-value "belgian")))
  (Thread/sleep 2500)
  (is (nil? (redis/get-value "belgian"))))

(deftest delete-a-key-test
  (redis/setex "belgian" "dubbel" 2)
  (redis/del "belgian")
  (is (nil? (redis/get-value"belgian"))))

(deftest sadd-srem-item-test
  (redis/sadd "beers" "ipa")
  (is (redis/smember? "beers" "ipa"))
  (redis/srem "beers" "ipa")
  (is (not (redis/smember? "beers" "ipa"))))

(deftest push-value-onto-queue-test
  (testing "pushes a String value onto a named queued"
    (is (= (redis/push "bacon" "eggs") 1)))

  (testing "uses a separate RedisConnection to push a String value"
    (let [redis-conn (redis/new-connection redis-test-settings)]
      (is (= (redis/push "pancakes" "syrup" redis-conn) 1))
      )))

(deftest rpush-value-onto-queue-test
  (testing "pushes a String value onto the tail of the named queue"
    (is (= (redis/rpush "chicken" "tacos") 1))
    (is (= "tacos" (redis/pop-to-processing "chicken"))))

  (testing "pushes a String value ahead of something already in the queue"
    (let [_ (redis/push "chicken" "fajitas")
          _ (redis/rpush "chicken" "pot pie")]
      (is (= "pot pie" (redis/pop-to-processing "chicken")))
      (is (= "fajitas" (redis/pop-to-processing "chicken")))
      )))

(deftest pop-value-from-queue-test
  (testing "consumes a value from a named queue"
    (let [_ (redis/push "mocha" "latte")
          value (redis/pop-to-processing "mocha")]
      (is (= value "latte"))))

  (testing "places the pop valued into the processing queue"
    (let [_ (redis/push "deep dish" "pizza")
          _ (redis/pop-to-processing "deep dish")
          values (redis/lrange "processing" 0 0)]
      (is (= (first values) "pizza")))
    )

  (testing "uses a separate RedisConnection to pop a value"
    (let [redis-conn (redis/new-connection redis-test-settings)
          _ (redis/push "salt" "pepper" redis-conn)]
      (is (= (redis/pop-to-processing "salt" redis-conn) "pepper"))
      )))

(deftest blocking-pop-value-test
  (testing "consumes a value from a named queue"
    (let [_ (redis/push "mocha" "latte")
          value (redis/blocking-pop "mocha")]
      (is (= value "latte"))))

  (testing "places the value from the queue into the named processing queue"
    (let [_ (redis/push "basil" "pesto")
          value (redis/blocking-pop "basil" "basil-processing" 2)
          value-from-processing (redis/processing-pop "basil-processing")]
      (is (= value value-from-processing)))))

(deftest remove-from-processing-test
  (testing "it removes the value from the processing queue"
    (let [original-value "latte"
          _ (redis/push "caramel" original-value)
          popped-value (redis/blocking-pop "caramel")
          _ (redis/remove-from-processing original-value)
          remaining-vals (redis/lrange "processing" 0 0)]
      (is (= popped-value "latte"))
      (is (empty? remaining-vals))
      )))

(deftest processing-pop-test
  (testing "it removes the next item off of the processing queue"
    (let [original-value "blintzes"
          _ (redis/push "cheese" original-value)
          popped-value (redis/blocking-pop "cheese")
          processing-pop-val (redis/processing-pop)]
      (is (= processing-pop-val original-value))))
  (testing "when there are multiple values on the processing queue"
    (let [first-value "blintzes"
          _ (redis/push "cheese" first-value)
          second-value "pierogies"
          _ (redis/push "cheese" second-value)
          _ (dorun (repeatedly 2 #(redis/blocking-pop "cheese")))
          processing-first-val (redis/processing-pop)
          processing-sec-val (redis/processing-pop)]
      (is (= processing-first-val first-value))
      (is (= processing-sec-val second-value)))
    ))

(deftest push-worker-runtime-test
  (testing "it prepends the latest runtime to the front of the list"
    (redis/push-worker-runtime "worker-1" 20)
    (redis/push-worker-runtime "worker-1" 21)
    (is (= [21 20] (redis/get-worker-runtimes "worker-1")))))
