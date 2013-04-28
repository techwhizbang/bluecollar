(ns bluecollar.event-processor
  (:require [bluecollar.redis-message-storage :as redis]))

(def ^:private keep-listening (atom true))

(defn start [queue-name]
  (while @keep-listening
    (let [value (redis/blocking-pop queue-name)]
      (prn "dispatch the worker here " value)
      )
    ))

(defn stop []
  (reset! keep-listening false))