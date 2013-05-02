(ns bluecollar.redis-message-storage
  (:require [taoensso.carmine :as redis])
  (:refer-clojure :exclude [pop]))

(defrecord RedisConnection [pool settings])

(def ^:private pool-and-settings (atom nil))

(def processing-queue
  "The name of the queue that items are pushed to until they are successfully processed.
   Feel free to change the name of this value if you see fit."
  (atom "bluecollar-processing-queue"))

(defmacro ^{:private true} with-redis-conn [redis-connection & body]
  `(redis/with-conn (:pool ~redis-connection) (:settings ~redis-connection) ~@body))

(defn redis-settings
  "The connection timeout setting is millisecond based.
   When specifying the connection timeout be sure that it is greater than the timeout specified
   for blocking operations."
  [{:keys [host port timeout db]}]
  (redis/make-conn-spec :host host :timeout timeout :port port :db db))

(defn redis-pool []
  (redis/make-conn-pool))

(defn startup [config]
  (reset! pool-and-settings
    (->RedisConnection (redis-pool) (redis-settings config))))

(defn new-connection [config]
  (->RedisConnection (redis-pool) (redis-settings config)))

(defn shutdown []
  (reset! pool-and-settings nil))

(defn flushdb
  ([] (with-redis-conn @pool-and-settings (redis/flushdb)))
  ([redis-conn] (with-redis-conn redis-conn (redis/flushdb))))

(defn lrange [queue-name start end]
  (with-redis-conn @pool-and-settings (redis/lrange queue-name start end)))

(defn processing-pop
  "Removes the last occurrence of the given value from the processing queue."
  ([value] (with-redis-conn @pool-and-settings (redis/lrem @processing-queue -1 value)))
  ([value redis-conn] (with-redis-conn redis-conn (redis/lrem @processing-queue -1 value))))

(defn push
  "Push a value into the named queue."
  ([queue-name value] (with-redis-conn @pool-and-settings (redis/lpush queue-name value)))
  ([queue-name value redis-conn] (with-redis-conn redis-conn (redis/lpush queue-name value))))

(defn pop
  "Pops a value from the queue and places the value into the processing queue."
  ([queue-name] (with-redis-conn @pool-and-settings (redis/rpoplpush queue-name @processing-queue)))
  ([queue-name redis-conn] (with-redis-conn redis-conn (redis/rpoplpush queue-name @processing-queue))))

(defn blocking-pop
  "Behaves identically to consume but will wait indefinitely until something is pushed to the queue."
  ([queue-name] (with-redis-conn @pool-and-settings (redis/brpoplpush queue-name @processing-queue 2)))
  ([queue-name timeout] (with-redis-conn @pool-and-settings (redis/brpoplpush queue-name @processing-queue timeout)))
  ([queue-name timeout redis-conn] (with-redis-conn redis-conn (redis/brpoplpush queue-name @processing-queue timeout))))
