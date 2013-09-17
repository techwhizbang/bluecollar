(ns bluecollar.job-sites
  (:use bluecollar.lifecycle)
  (:require [bluecollar.redis :as redis]
            [bluecollar.foreman :as foreman]
            [bluecollar.job-plans :as plan]
            [cheshire.core :as json]
            [clojure.tools.logging :as logger]))

(defrecord JobSite [#^String queue-name #^bluecollar.foreman.Foreman foreman])

(extend-type JobSite
  Lifecycle
  
  (startup [this] 
    (logger/info "Starting JobSite: " (:queue-name this))
    (logger/info "The JobSite Foreman is " (:foreman this))
    (startup (:foreman this)))

  (shutdown [this]
    (logger/info "Stopping JobSite: " (:queue-name this))
    (shutdown (:foreman this))
    ))

(defn new-job-site [queue-name worker-count]
  (->JobSite queue-name (foreman/new-foreman queue-name worker-count)))

