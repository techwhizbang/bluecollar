(ns bluecollar.job-sites
  (:use bluecollar.lifecycle)
  (:require [bluecollar.redis :as redis]
            [bluecollar.foreman :as foreman]
            [bluecollar.job-plans :as plan]
            [cheshire.core :as json]
            [clojure.tools.logging :as logger]))

(defrecord JobSite [#^String site-name #^bluecollar.foreman.Foreman foreman continue-running])

(extend-type JobSite
  Lifecycle
  
  (startup [this] 
    (logger/info "Starting JobSite: " (:site-name this))
    (logger/info "The JobSite Foreman is " (:foreman this))
    (startup (:foreman this))    
    (future
      (while @(:continue-running this)
        (try
          (let [value (redis/blocking-pop (:site-name this))]
            (if (and (not (nil? value)) (not (coll? value)))
              (do
                (logger/info "JobSite" (:site-name this) "received a message " value)
                (foreman/dispatch-work (:foreman this) (plan/from-json value)))))
          (catch Exception ex
            (logger/error ex)))
      )))

  (shutdown [this]
    (logger/info "Stopping JobSite: " (:site-name this))
    (reset! (:continue-running this) false)
    (shutdown (:foreman this))
    ))

(defn new-job-site [site-name worker-count]
  (->JobSite site-name (foreman/new-foreman worker-count) (atom true)))
