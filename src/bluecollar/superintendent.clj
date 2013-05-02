(ns bluecollar.superintendent
  (:require [bluecollar.redis-message-storage :as redis]
    [bluecollar.foreman :as foreman]
    [bluecollar.job-plans :as plan]
    [cheshire.core :as json]))

(def ^:private keep-everyone-working (atom true))

(defn start
  "The superintendent gets messages from the given queue.
   Like all management they delegate work. So the superintendent
   translates the message into a job plan for their foreman
   to dispatch."
  [queue-name]
  (do
    (reset! keep-everyone-working true)
    (while @keep-everyone-working
      (let [value (redis/blocking-pop queue-name)]
        (if-not (and (nil? value) (vector? value))
          (let [plan-map (plan/from-json value)]
            (foreman/dispatch-work plan-map))
          )))))

(defn stop []
  (reset! keep-everyone-working false))

