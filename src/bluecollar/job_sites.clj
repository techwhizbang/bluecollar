(ns bluecollar.job-sites
  (:use bluecollar.lifecycle)
  (:require [bluecollar.redis :as redis]
            [bluecollar.foreman :as foreman]
            [bluecollar.job-plans :as plan]
            [cheshire.core :as json]
            [clojure.tools.logging :as logger]))

(def ^:private keep-everyone-working (atom true))

(defrecord JobSite [#^String site-name #^bluecollar.foreman.Foreman foreman #^int worker-count]
  Lifecycle
  (startup [this] 
    (reset! keep-everyone-working true)
    (logger/info "Starting JobSite: " (:site-name this))
    (foreman/start-workers (:worker-count this))
    (while @keep-everyone-working
      (let [value (redis/blocking-pop (:site-name this))]
        (if (and (not (nil? value)) (not (coll? value)))
          (foreman/dispatch-work (plan/from-json value)))
        )))
  (shutdown [this]
    (reset! keep-everyone-working false)
    (logger/info "Stopping JobSite: " (:site-name this))
    (foreman/stop-workers)))

(defn new-job-site [site-name worker-count]
  (->JobSite site-name (foreman/new-foreman worker-count) worker-count))

(defn start
  "On a JobSite it is the Foreman's job to start the appropriate number of workers and
   to keep the workers busy by dispatching job plans. The Foreman continues to do this
   until the JobSite is shutdown."
  [queue-name worker-count]
  (reset! keep-everyone-working true)
  (logger/info "Starting the worker thread pool for " queue-name)
  (foreman/start-workers worker-count)
  (while @keep-everyone-working
    (let [value (redis/blocking-pop queue-name)]
      (if (and (not (nil? value)) (not (coll? value)))
        (foreman/dispatch-work (plan/from-json value)))
      )))

(defn stop []
  (reset! keep-everyone-working false)
  (logger/info "Stopping the worker thread pool")
  (foreman/stop-workers))

