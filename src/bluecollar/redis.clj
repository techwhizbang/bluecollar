(ns bluecollar.redis
  (:require [taoensso.carmine :as redis-client]
            [taoensso.carmine.connections :as redis-conn]
            [bluecollar.keys-and-queues :as keys-qs])
  (:refer-clojure :exclude [pop]))

(defrecord RedisConnection [pool settings])

(def pool-and-settings (atom nil))

(def config (atom {:host "127.0.0.1" :port 6379 :db 0 :timeout 5000}))

(defn set-config [redis-configs]
  (reset! config redis-configs))

(defmacro with-redis-conn [redis-connection & body]
  `(redis-client/wcar {:pool (:pool ~redis-connection) :spec (:settings ~redis-connection)} ~@body))

(defmacro with-transaction [& body]
  `(with-redis-conn (deref pool-and-settings) (redis-client/multi) ~@body (redis-client/exec)))

(defmulti redis-settings (fn [config] (type config)))

(defmethod redis-settings clojure.lang.APersistentMap [{:keys [host port timeout db]}] 
  (redis-conn/conn-spec {:host host :timeout timeout :port port :db db}))

(defmethod redis-settings clojure.lang.Atom [config]
  (redis-settings @config))

(defn redis-pool 
  ([] (redis-pool 10))
  ([pool-size] (redis-conn/conn-pool {:max-total -1 :min-idle pool-size :max-active pool-size})))

(defn startup
  ([] (startup @config))
  ([config] (reset! pool-and-settings (->RedisConnection (redis-pool) (redis-settings config)))))

(defn ping [] (with-redis-conn @pool-and-settings (redis-client/ping)))

(defn new-connection 
  ([] (new-connection config))
  ([config] (new-connection config 10))
  ([config pool-size] (->RedisConnection (redis-pool pool-size) (redis-settings config))))

(defn shutdown []
  (reset! pool-and-settings nil))

(defn flushdb
  ([] (flushdb @pool-and-settings))
  ([redis-conn] (with-redis-conn redis-conn (redis-client/flushdb))))

(defn lrange [queue-name start end]
  (with-redis-conn @pool-and-settings (redis-client/lrange (keys-qs/fetch-queue queue-name) start end)))

(defn failure-retry-cnt [uuid]
  (let [cnt (with-redis-conn @pool-and-settings (redis-client/hget (keys-qs/failure-retry-counter-key) uuid))]
    (if (nil? cnt)
      0
      (Integer/parseInt cnt))))

(defn failure-retry-inc
  ([uuid] (failure-retry-inc uuid @pool-and-settings))
  ([uuid redis-conn] (with-redis-conn redis-conn (redis-client/hincrby (keys-qs/failure-retry-counter-key) uuid 1))))

(defn failure-retry-del
  ([uuid] (failure-retry-del uuid @pool-and-settings))
  ([uuid redis-conn] (with-redis-conn redis-conn (redis-client/hdel (keys-qs/failure-retry-counter-key) uuid))))

(defn push-worker-runtime
  ^{:doc "Prepends the worker runtime in milliseconds."}
  [worker-name runtime-in-millisecs]
  (with-redis-conn @pool-and-settings 
    (redis-client/lpush (keys-qs/worker-runtimes-key (name worker-name)) runtime-in-millisecs)
    (redis-client/ltrim (keys-qs/worker-runtimes-key (name worker-name)) 0 999)))

(defn get-worker-runtimes
  ^{:doc "Returns the last 1000 runtimes recorded for the given worker."}
  [worker-name]
  (let [runtimes (with-redis-conn @pool-and-settings (redis-client/lrange (keys-qs/worker-runtimes-key (name worker-name)) 0 999))]
    (vec (map (fn [runtime] (Integer/parseInt runtime)) runtimes))))

(defn failure-total-cnt
  ^{:doc "Returns the number of failed processed jobs."}
  []
  (let [cnt (with-redis-conn @pool-and-settings (redis-client/get (keys-qs/failure-total-counter-key)))]
    (if (nil? cnt)
      0
      (Integer/parseInt cnt))))

(defn failure-total-inc
  ^{:doc "Increments the number of failed jobs by 1."}
  ([] (failure-total-inc @pool-and-settings))
  ([redis-conn] (with-redis-conn redis-conn (redis-client/incr (keys-qs/failure-total-counter-key)))))

(defn failure-total-del
  ^{:doc "Deletes the key that stores the total count of failed jobs."}
  ([] (failure-total-del @pool-and-settings))
  ([redis-conn] (with-redis-conn redis-conn (redis-client/del (keys-qs/failure-total-counter-key)))))

(defn success-total-cnt
  ^{:doc "Returns the number of successfully processed jobs."}
  []
  (let [cnt (with-redis-conn @pool-and-settings (redis-client/get (keys-qs/success-total-counter-key)))]
    (if (nil? cnt)
      0
      (Integer/parseInt cnt))))

(defn success-total-inc
  ^{:doc "Increments the number of successfully processed jobs by 1."}
  ([] (success-total-inc @pool-and-settings))
  ([redis-conn] (with-redis-conn redis-conn (redis-client/incr (keys-qs/success-total-counter-key)))))

(defn success-total-del
  ^{:doc "Delete the key that stores the number of successfully processed jobs."}
  ([] (success-total-del @pool-and-settings))
  ([redis-conn] (with-redis-conn redis-conn (redis-client/del (keys-qs/success-total-counter-key)))))

(defn push
  ^{:doc "Push a value to the head of the named queue."}
  ([queue-name value] (push queue-name value @pool-and-settings))
  ([queue-name value redis-conn] (with-redis-conn redis-conn (redis-client/lpush (keys-qs/fetch-queue queue-name) value))))

(defn push-no-conn
  ^{:doc "Push a value to the head of the named queue."}
  [queue-name value] (redis-client/lpush (keys-qs/fetch-queue queue-name) value))

(defn rpush-no-conn
  ^{:doc "Push a value to the tail of the queue."}
  [queue-name value] (redis-client/rpush (keys-qs/fetch-queue queue-name) value))

(defn rpush
  ^{:doc "Push a value to the tail of the queue."}
  ([queue-name value] (rpush queue-name value @pool-and-settings))
  ([queue-name value redis-conn] (with-redis-conn redis-conn (redis-client/rpush (keys-qs/fetch-queue queue-name) value))))

(defn rpop
  ^{:doc "Pops a value from the tail of the queue."}
  ([queue-name] (rpop queue-name @pool-and-settings))
  ([queue-name redis-conn] (with-redis-conn redis-conn (redis-client/rpop (keys-qs/fetch-queue queue-name)))))

(defn setex
  ^{:doc "Sets a value with an expiration based on the given key."}
  ([k v expiry] (setex k v expiry @pool-and-settings))
  ([k v expiry redis-conn] (with-redis-conn redis-conn (redis-client/setex k expiry v))))

(defn del
  ^{:doc "Deletes a value based on the given key."}
  ([k] (del k @pool-and-settings))
  ([k redis-conn] (with-redis-conn redis-conn (redis-client/del k))))

(defn get-value
  ^{:doc "Gets a value based on the given key."}
  ([k] (get-value k @pool-and-settings))
  ([k redis-conn] (with-redis-conn redis-conn (redis-client/get k))))

(defn sadd 
  ^{:doc "Adds an item to a set"}
  ([a-set item] (sadd a-set item @pool-and-settings))
  ([a-set item redis-conn] (with-redis-conn redis-conn (redis-client/sadd a-set item))))

(defn srem
  ^{:doc "Removes an item from a set"}
  ([a-set item] (srem a-set item @pool-and-settings))
  ([a-set item redis-conn] (with-redis-conn redis-conn (redis-client/srem a-set item))))

(defn smember?
  ^{:doc "Returns true|false depending on if the item is a member of the set."}
  ([a-set item] (smember? a-set item @pool-and-settings))
  ([a-set item redis-conn] (> (with-redis-conn redis-conn (redis-client/sismember a-set item)) 0)))

(defn smembers
  ^{:doc "Returns the members of the given set name."}
  ([a-set] (smembers a-set @pool-and-settings))
  ([a-set redis-conn] (with-redis-conn redis-conn (redis-client/smembers a-set))))

(defn brpop
  ^{:doc "Blocking operation that pops a value from the tail of the queue."}
  ([queue-name] (brpop queue-name 2))
  ([queue-name timeout] (brpop queue-name timeout @pool-and-settings))
  ([queue-name timeout redis-conn] (second (with-redis-conn redis-conn (redis-client/brpop (keys-qs/fetch-queue queue-name) 2)))))

(defn pop-to-processing
  ^{:doc "Pops a value from the queue and places the value into the processing queue."}
  ([queue-name] (pop-to-processing queue-name @pool-and-settings))
  ([queue-name redis-conn] (with-redis-conn redis-conn (redis-client/rpoplpush (keys-qs/fetch-queue queue-name) (keys-qs/fetch-queue "processing")))))

(defn processing-pop
  ^{:doc "Pops a value from the processing queue. Used mostly for recovery purposes at this point."}
  ([] (rpop "processing"))
  ([processing-queue-name] (rpop processing-queue-name @pool-and-settings))
  ([processing-queue-name redis-conn] (rpop processing-queue-name redis-conn)))

(defn remove-from-processing
  ^{:doc "Removes the last occurrence of the given value from the processing queue."}
  ([value] (remove-from-processing value "processing"))
  ([value queue] (remove-from-processing value queue @pool-and-settings))
  ([value queue redis-conn] (with-redis-conn redis-conn (redis-client/lrem (keys-qs/fetch-queue queue) -1 value))))

(defn blocking-pop
  ^{:doc "Behaves identically to consume but will wait for timeout or until something is pushed to the queue."}
  ([queue-name] (blocking-pop queue-name 2))
  ([queue-name timeout] (blocking-pop queue-name "processing" timeout))
  ([queue-name processing-queue-name timeout] (blocking-pop queue-name processing-queue-name timeout @pool-and-settings))
  ([queue-name processing-queue-name timeout redis-conn] (with-redis-conn redis-conn (redis-client/brpoplpush (keys-qs/fetch-queue queue-name) (keys-qs/fetch-queue processing-queue-name) timeout))))
