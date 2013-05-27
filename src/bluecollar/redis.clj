(ns bluecollar.redis
  (:require [taoensso.carmine :as redis-client])
  (:refer-clojure :exclude [pop]))

(defrecord RedisConnection [pool settings])

(def ^:private pool-and-settings (atom nil))

(def processing-queue
  "The name of the queue that items are pushed to until they are successfully processed.
   Feel free to change the name of this value if you see fit."
  (atom "bluecollar-processing-queue"))

(def failures-hash
  "The name of the hash where the count of failed jobs is stored. Feel free to change
  the name of this value if you see fit."
  (atom "bluecollar-failed-jobs"))

(defmacro ^{:private true} with-redis-conn [redis-connection & body]
  `(redis-client/with-conn (:pool ~redis-connection) (:settings ~redis-connection) ~@body))

(defn redis-settings
  "The connection timeout setting is millisecond based.
   When specifying the connection timeout be sure that it is greater than the timeout specified
   for blocking operations."
  [{:keys [host port timeout db]}]
  (redis-client/make-conn-spec :host host :timeout timeout :port port :db db))

(defn redis-pool []
  (redis-client/make-conn-pool))

(defn startup [config]
  (reset! pool-and-settings
    (->RedisConnection (redis-pool) (redis-settings config))))

(defn new-connection [config]
  (->RedisConnection (redis-pool) (redis-settings config)))

(defn shutdown []
  (reset! pool-and-settings nil))

(defn flushdb
  ([] (flushdb @pool-and-settings))
  ([redis-conn] (with-redis-conn redis-conn (redis-client/flushdb))))

(defn lrange [queue-name start end]
  (with-redis-conn @pool-and-settings (redis-client/lrange queue-name start end)))

(defn failure-count [uuid]
  (let [cnt (with-redis-conn @pool-and-settings (redis-client/hget @failures-hash uuid))]
    (if (nil? cnt)
      0
      (Integer/parseInt cnt))))

(defn failure-count-total []
  (let [cnts (with-redis-conn @pool-and-settings (redis-client/hvals @failures-hash))
        cnts-as-ints (map #(Integer/parseInt %) cnts)]
        (apply + cnts-as-ints)))

(defn failure-inc
  ([uuid] (failure-inc uuid @pool-and-settings))
  ([uuid redis-conn] (with-redis-conn redis-conn (redis-client/hincrby @failures-hash uuid 1))))

(defn processing-pop
  "Removes the last occurrence of the given value from the processing queue."
  ([value] (processing-pop value @pool-and-settings))
  ([value redis-conn] (with-redis-conn redis-conn (redis-client/lrem @processing-queue -1 value))))

(defn push
  "Push a value into the named queue."
  ([queue-name value] (push queue-name value @pool-and-settings))
  ([queue-name value redis-conn] (with-redis-conn redis-conn (redis-client/lpush queue-name value))))

(defn pop
  "Pops a value from the queue and places the value into the processing queue."
  ([queue-name] (pop queue-name @pool-and-settings))
  ([queue-name redis-conn] (with-redis-conn redis-conn (redis-client/rpoplpush queue-name @processing-queue))))

(defn blocking-pop
  "Behaves identically to consume but will wait for timeout or until something is pushed to the queue."
  ([queue-name] (blocking-pop queue-name 2))
  ([queue-name timeout] (blocking-pop queue-name timeout @pool-and-settings))
  ([queue-name timeout redis-conn] (with-redis-conn redis-conn (redis-client/brpoplpush queue-name @processing-queue timeout))))
