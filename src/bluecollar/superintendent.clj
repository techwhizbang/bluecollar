(ns bluecollar.superintendent
  (:require [bluecollar.redis :as redis]
            [bluecollar.foreman :as foreman]
            [bluecollar.job-plans :as plan]
            [cheshire.core :as json]))

(def ^:private keep-everyone-working (atom true))

(defn start
  "Just like all management the superintendent likes to delegate work.
   The superintendent informs the foreman how many workers are required.
   The foreman starts the appropriate number of workers and waits for further instruction.
   Throughout the day the superintendent receives messages from \"upper management\".
   The superintendent translates those messages into job plans for the foreman.
   The foreman takes the job plans and dispatches the workers accordingly."
  [queue-name worker-count]
  (reset! keep-everyone-working true)
  (foreman/start-workers worker-count)
  (while @keep-everyone-working
    (let [value (redis/blocking-pop queue-name)]
      (if (and (not (nil? value)) (not (coll? value)))
        (foreman/dispatch-work (plan/from-json value)))
      )))

(defn stop []
  (reset! keep-everyone-working false)
  (foreman/stop-workers))

