(ns bluecollar.master-job-site
  (:use bluecollar.lifecycle)
  (:require [bluecollar.redis :as redis]
            [bluecollar.job-plans :as plan]
            [bluecollar.workers-union :as workers-union]
            [bluecollar.keys-and-queues :as keys-qs]
            [cheshire.core :as json]
            [clojure.tools.logging :as logger]))

(defrecord MasterJobSite [continue-running])

(defn- handler [value]
  (if (and (not (nil? value)) (not (coll? value)))
    (let [job-plan (plan/from-json value)
          worker (workers-union/find-worker (:worker job-plan))
          intended-queue (:queue worker)]
      (redis/push intended-queue value)
      (redis/remove-from-processing value keys-qs/master-processing-queue-name)
      )))

(extend-type MasterJobSite
  Lifecycle
  
  (startup [this] 
    (logger/info "Starting the MasterJobSite")
    (future
      (while @(:continue-running this)
        (try
          (let [queue keys-qs/master-queue-name
                processing-queue keys-qs/master-processing-queue-name]
            (handler (redis/blocking-pop queue processing-queue 2)))
          (catch Exception ex
            (logger/error ex)))
      )))

  (shutdown [this]
    (logger/info "Stopping the MasterJobSite")
    (reset! (:continue-running this) false)
    ))

(defn new-master-job-site []
  (->MasterJobSite (atom true)))
