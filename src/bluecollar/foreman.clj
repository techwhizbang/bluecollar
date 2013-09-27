(ns bluecollar.foreman
  (:use bluecollar.lifecycle)
  (:import java.util.concurrent.Executors
           java.util.concurrent.ExecutorService)
  (:require [bluecollar.workers-union :as workers-union]
            [bluecollar.job-plans :as plan]
            [bluecollar.redis :as redis]
            [bluecollar.keys-and-queues :as keys-qs]
            [clojure.tools.logging :as logger]))

(declare 
  dispatch-scheduled-worker
  dispatch-fixed-worker
  dispatch
  fetch-job-plan
  fixed-workers-available?
  scheduled-workers-available?)

(defrecord Foreman [#^String queue-name #^int worker-count fixed-pool scheduled-pool redis-conn continue-running])

(extend-type Foreman
  Lifecycle

  (startup [this]
    (logger/info "Starting up foreman for queue" (:queue-name this))
    (let [thread-cnt (:worker-count this)
          fixed-pool (. Executors newFixedThreadPool thread-cnt)
          scheduled-pool (. Executors newScheduledThreadPool thread-cnt)]
      (reset! (:continue-running this) true)
      (reset! (:fixed-pool this) fixed-pool)
      (reset! (:scheduled-pool this) scheduled-pool)
      (reset! (:redis-conn this) (redis/new-connection))
      (.prestartAllCoreThreads @(:scheduled-pool this))
      (.prestartAllCoreThreads @(:fixed-pool this))

      (dotimes [x thread-cnt] (future (fetch-job-plan this)))
      ))

  (shutdown [this]
    (logger/info "Shutting down foreman")
    (reset! (:continue-running this) false)
    (reset! (:redis-conn this) nil)
    (.shutdown @(:fixed-pool this))
    (.shutdown @(:scheduled-pool this))))

(defn dispatch-scheduled-worker [#^bluecollar.foreman.Foreman a-foreman #^bluecollar.job_plans.JobPlan job-plan]
  "Dispatches a scheduled job plan to the dedicated scheduling thread pool."
  (let [scheduled-runtime (plan/secs-to-runtime job-plan)
        runnable-job-plan (plan/as-runnable job-plan (fn [] (fetch-job-plan a-foreman)))]
    (logger/debug "The foreman is " a-foreman)
    (logger/debug "The scheduled pool is " (:scheduled-pool a-foreman))
    (.schedule @(:scheduled-pool a-foreman) runnable-job-plan scheduled-runtime (java.util.concurrent.TimeUnit/SECONDS))))

(defn dispatch-fixed-worker [#^bluecollar.foreman.Foreman a-foreman #^bluecollar.job_plans.JobPlan job-plan]
  "Dispatches a job plan to the worker pool."
  (logger/debug "The foreman is " a-foreman)
  (logger/debug "The fixed pool is " (:fixed-pool a-foreman))
  (.execute @(:fixed-pool a-foreman) (plan/as-runnable job-plan (fn [] (fetch-job-plan a-foreman)))))

(defn dispatch [#^bluecollar.foreman.Foreman a-foreman #^bluecollar.job_plans.JobPlan job-plan]
  "Dispatch the appropriate worker based on the given job-plan."
  (logger/info "Dispatching a worker for JobPlan with UUID: " (:uuid job-plan))
  (if (plan/schedulable? job-plan)
      (dispatch-scheduled-worker a-foreman job-plan)
      (dispatch-fixed-worker a-foreman job-plan)))

(defn- job-plan-handler [a-foreman job-plan-json]
  (logger/debug "on-receipt-handler" job-plan-json)
  (let [queue (:queue-name a-foreman)
          job-plan (plan/from-json job-plan-json)
          job-uuid (:uuid job-plan)
          worker-set (keys-qs/worker-set-name queue)]
     
      (redis/sadd worker-set job-uuid)
      ; expire in 7 days if it hasn't been processed yet
      (redis/setex (keys-qs/worker-key queue job-uuid) job-plan-json (* 60 60 24 7))
     
      (dispatch a-foreman job-plan)))

(defn fetch-job-plan [a-foreman]
  (let [continue-running? @(:continue-running a-foreman)]
    ; don't bother fetching anything if the foreman is shutting down
    (if continue-running?
      ; if there are workers waiting around for work then proceed
      (do
        (if (fixed-workers-available? a-foreman)
          (do
            (logger/debug "workers are available? " (fixed-workers-available? a-foreman))
            (let [queue (:queue-name a-foreman)
                  redis-conn @(:redis-conn a-foreman)
                  job-plan-json (redis/brpop queue 2 redis-conn)]
              (if (and (not (nil? job-plan-json)) (not (coll? job-plan-json)))
                (job-plan-handler a-foreman job-plan-json)
                ; if nothing was found then recur and try again...
                (recur a-foreman))))))
      ; all the workers were busy, so recur and try again... 
      (recur a-foreman))))

(defn fixed-worker-count [#^bluecollar.foreman.Foreman a-foreman]
  "Returns the total number of workers."
  (.getPoolSize @(:fixed-pool a-foreman)))

(defn fixed-workers-available? [#^bluecollar.foreman.Foreman a-foreman]
  "Returns true if there are any inactive workers in the fixed-pool."
  (> (- (fixed-worker-count a-foreman) (.getActiveCount @(:fixed-pool a-foreman))) 0))

(defn scheduled-worker-count [#^bluecollar.foreman.Foreman a-foreman]
  "Returns the total number of schedulable workers"
  (.getPoolSize @(:scheduled-pool a-foreman)))

(defn scheduled-workers-available? [#^bluecollar.foreman.Foreman a-foreman]
  "Returns true if there are any inactive workers in the scheduled-pool"
  (> (- (scheduled-worker-count a-foreman) (.getActiveCount @(:scheduled-pool a-foreman))) 0))

(defn new-foreman [queue-name worker-count] (->Foreman queue-name worker-count (atom nil) (atom nil) (atom nil) (atom true)))




