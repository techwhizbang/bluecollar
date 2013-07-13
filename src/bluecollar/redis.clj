(ns bluecollar.redis
  (:require [taoensso.carmine :as redis-client])
  (:refer-clojure :exclude [pop]))

(defrecord RedisConnection [pool settings])

(def ^:private pool-and-settings (atom nil))

(def redis-key-prefix
  ^{:doc "The prefix to all keys, lists, and data structures that pass through Redis.
          Feel free to change the name of this value if you see fit."}
  (atom "bluecollar"))

(defn setup-key-prefix [prefix] (reset! redis-key-prefix (or prefix "bluecollar")))

(def processing-queue (atom (str @redis-key-prefix ":processing-queue:default")))

(defn setup-processing-queue [instance-name]
  (reset! processing-queue (str @redis-key-prefix ":processing-queue:" (or instance-name "default"))))

(defmacro ^{:private true} with-redis-conn [redis-connection & body]
  `(redis-client/with-conn (:pool ~redis-connection) (:settings ~redis-connection) ~@body))

(defn redis-settings
  ^{:doc "The connection timeout setting is millisecond based.
          When specifying the connection timeout be sure that it is greater than the timeout specified
          for blocking operations."}
  [{:keys [host port timeout db]}]
  (redis-client/make-conn-spec :host host :timeout timeout :port port :db db))

(defn redis-pool []
  (redis-client/make-conn-pool))

(defn startup [config]
  (reset! pool-and-settings
    (->RedisConnection (redis-pool) (redis-settings config))))

(defn ping [] (with-redis-conn @pool-and-settings (redis-client/ping)))

(defn new-connection [config]
  (->RedisConnection (redis-pool) (redis-settings config)))

(defn shutdown []
  (reset! pool-and-settings nil))

(defn flushdb
  ([] (flushdb @pool-and-settings))
  ([redis-conn] (with-redis-conn redis-conn (redis-client/flushdb))))

(defn lrange [queue-name start end]
  (with-redis-conn @pool-and-settings (redis-client/lrange queue-name start end)))

(defn failure-retry-counter
  ^{:doc "The name of the hash where the count of retryable failed jobs is stored."}
  [] (str @redis-key-prefix ":failure-retry-counter"))

(defn failure-retry-cnt [uuid]
  (let [cnt (with-redis-conn @pool-and-settings (redis-client/hget (failure-retry-counter) uuid))]
    (if (nil? cnt)
      0
      (Integer/parseInt cnt))))

(defn failure-retry-inc
  ([uuid] (failure-retry-inc uuid @pool-and-settings))
  ([uuid redis-conn] (with-redis-conn redis-conn (redis-client/hincrby (failure-retry-counter) uuid 1))))

(defn failure-retry-del
  ([uuid] (failure-retry-del uuid @pool-and-settings))
  ([uuid redis-conn] (with-redis-conn redis-conn (redis-client/hdel (failure-retry-counter) uuid))))

(defn push-worker-runtime
  "Prepends the worker runtime in milliseconds."
  [worker-name runtime-in-millisecs]
  (with-redis-conn @pool-and-settings (redis-client/lpush (str @redis-key-prefix ":worker-runtimes:" worker-name) runtime-in-millisecs)))

(defn get-worker-runtimes
  "Returns the last 1000 runtimes recorded for the given worker."
  [worker-name]
  (let [runtimes (with-redis-conn @pool-and-settings (redis-client/lrange (str @redis-key-prefix ":worker-runtimes:" worker-name) 0 999))]
    (vec (map (fn [runtime] (Integer/parseInt runtime)) runtimes))))

(defn failure-total-counter
  ^{:doc "The name of the key where the total count of failed jobs is stored."}
  [] (str @redis-key-prefix ":failure-total-counter"))

(defn failure-total-cnt
  "Returns the number of failed processed jobs."
  []
  (let [cnt (with-redis-conn @pool-and-settings (redis-client/get (failure-total-counter)))]
    (if (nil? cnt)
      0
      (Integer/parseInt cnt))))

(defn failure-total-inc
  "Increments the number of failed jobs by 1."
  ([] (failure-total-inc @pool-and-settings))
  ([redis-conn] (with-redis-conn redis-conn (redis-client/incr (failure-total-counter)))))

(defn failure-total-del
  "Deletes the key that stores the total count of failed jobs."
  ([] (failure-total-del @pool-and-settings))
  ([redis-conn] (with-redis-conn redis-conn (redis-client/del (failure-total-counter)))))

(defn success-total-counter
  ^{:doc "The name of the key where the total count of successful jobs is stored."}
  [] (str @redis-key-prefix ":success-total-counter"))

(defn success-total-cnt
  "Returns the number of successfully processed jobs."
  []
  (let [cnt (with-redis-conn @pool-and-settings (redis-client/get (success-total-counter)))]
    (if (nil? cnt)
      0
      (Integer/parseInt cnt))))

(defn success-total-inc
  "Increments the number of successfully processed jobs by 1."
  ([] (success-total-inc @pool-and-settings))
  ([redis-conn] (with-redis-conn redis-conn (redis-client/incr (success-total-counter)))))

(defn success-total-del
  "Delete the key that stores the number of successfully processed jobs."
  ([] (success-total-del @pool-and-settings))
  ([redis-conn] (with-redis-conn redis-conn (redis-client/del (success-total-counter)))))

(defn remove-from-processing
  "Removes the last occurrence of the given value from the processing queue."
  ([value] (remove-from-processing value @pool-and-settings))
  ([value redis-conn] (with-redis-conn redis-conn (redis-client/lrem @processing-queue -1 value))))

(defn push
  "Push a value to the head of the named queue."
  ([queue-name value] (push queue-name value @pool-and-settings))
  ([queue-name value redis-conn] (with-redis-conn redis-conn (redis-client/lpush queue-name value))))

(defn rpush
  "Push a value to the tail of the queue."
  ([queue-name value] (rpush queue-name value @pool-and-settings))
  ([queue-name value redis-conn] (with-redis-conn redis-conn (redis-client/rpush queue-name value))))

(defn pop-to-processing
  "Pops a value from the queue and places the value into the processing queue."
  ([queue-name] (pop-to-processing queue-name @pool-and-settings))
  ([queue-name redis-conn] (with-redis-conn redis-conn (redis-client/rpoplpush queue-name @processing-queue))))

(defn processing-pop
  "Pops a value from the processing queue. Used mostly for recovery purposes at this point."
  ([] (processing-pop @pool-and-settings))
  ([redis-conn] (with-redis-conn redis-conn (redis-client/rpop @processing-queue))))

(defn blocking-pop
  "Behaves identically to consume but will wait for timeout or until something is pushed to the queue."
  ([queue-name] (blocking-pop queue-name 2))
  ([queue-name timeout] (blocking-pop queue-name timeout @pool-and-settings))
  ([queue-name timeout redis-conn] (with-redis-conn redis-conn (redis-client/brpoplpush queue-name @processing-queue timeout))))
