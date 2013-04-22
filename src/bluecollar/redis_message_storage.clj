(ns bluecollar.redis-message-storage
  (:use bluecollar.message-storage)
  (:require [taoensso.carmine :as redis]))

(def ^:private pool (atom nil))
(def ^:private settings (atom nil))

(defmacro redis-connection [& body] 
  `(redis/with-conn @pool @settings ~@body))

(defrecord RedisMessageStorage []
  IMessageStorage
  (connection-pool [this redis-config]
    (do
      (if (nil? @settings)
      	(reset! settings (redis/make-conn-spec 
                              :host (redis-config :host) 
      												:timeout (redis-config :timeout) 
      												:port (redis-config :port) 
      												:db (redis-config :db))))

      (if (nil? @pool)
        (reset! pool (redis/make-conn-pool)))

    ))
  (flushdb [this] (redis-connection (redis/flushdb))) 
  (push [this queue-name value] (redis-connection (redis/lpush queue-name value)))
  (consume [this queue-name] (redis-connection (redis/rpop queue-name)))
)