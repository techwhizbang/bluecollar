(ns bluecollar.client
  "The client namespace for the bluecollar library. 

   Use bluecollar.client to send asynchronous jobs for workers 
   previously specified at the startup of bluecollar (see bluecollar.core).

   Assuming :fibonacci-worker takes a single argument, namely the number of 
   Fibonacci numbers to calculate in a sequence, and it was included in your 
   worker specifications during the startup from bluecollar.core, 
   the client can send :fibonacci-worker a job just like this:

   => (use 'bluecollar.client)
   => (async-job-for :fibonacci-worker [20])

   If you wanted the :fibonacci-worker to process the job asynchronously with a 
   specific time in the future, you could also provide an ISO-8601 formatted
   time. So instead of being processed as soon as possible, it would execute
   no sooner than 2013-06-24T23:59:59.000Z.

   => (async-job-for :fibonacci-worker [400] \"2013-06-23T21:44:32.391Z\")
   "
  (:use [bluecollar.job-plans :only [async-job-plan]]))

(defn async-job-for
  ^{:doc "Send a registered worker a job to process asynchronously.
          The args vector must match the arity of the function originally
          associated to the registered worker.
          Optionally a scheduled runtime can be specified as an ISO-8601 formatted string."}
  ([worker-name #^clojure.lang.PersistentVector args] 
    (async-job-plan worker-name args))
  ([worker-name #^clojure.lang.PersistentVector args scheduled-runtime]
    (async-job-plan worker-name args scheduled-runtime)))

;TODO allow the registered worker to process the job inline
; (defn inline-job-for)