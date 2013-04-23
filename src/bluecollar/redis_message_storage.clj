(ns bluecollar.redis-message-storage
  (:require [taoensso.carmine :as redis]))

(defrecord RedisConnection [pool settings])

(def ^:private pool-and-settings (atom nil))

(def processing-queue (atom "bluecollar-processing-queue"))

(defmacro ^{:private true} with-redis-conn [redis-connection & body] 
  `(redis/with-conn (:pool ~redis-connection) (:settings ~redis-connection) ~@body))

(defn redis-settings [{:keys [host port timeout db]}]
  (redis/make-conn-spec :host host :timeout timeout :port port :db db))

(defn redis-pool []
  (redis/make-conn-pool))

(defn startup [config]
  (reset! pool-and-settings 
    (->RedisConnection (redis-pool) (redis-settings config))))

(defn flushdb 
  ([] (with-redis-conn @pool-and-settings (redis/flushdb)))
  ([redis-conn] (with-redis-conn redis-conn (redis/flushdb))))

(defn lrange [queue-name start end] 
  (with-redis-conn @pool-and-settings (redis/lrange queue-name start end)))

(defn push 
  ([queue-name value] (with-redis-conn @pool-and-settings (redis/lpush queue-name value)))
  ([redis-conn queue-name value] (with-redis-conn redis-conn (redis/lpush queue-name value))))

(defn consume 
  ([queue-name] (with-redis-conn @pool-and-settings (redis/rpoplpush queue-name @processing-queue)))
  ([redis-conn queue-name] (with-redis-conn redis-conn (redis/rpoplpush queue-name @processing-queue))))
