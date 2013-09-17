(ns example-app.bluecollar-core-and-client
  "This is an example application where bluecollar.core and bluecollar.client
   are side by side in a single process. In other words, you are sending jobs to be done
   and processing them in the same process.

   In some situtations where you're processing intensive jobs you would choose to run one
   or many bluecollar.core processes on many servers separate from your web or service layer.
   See bluecollar-core-only.clj for an of this.

   To start:
   => lein run -m example-app.bluecollar-core-and-client"

  (use bluecollar.core
       bluecollar.client
       compojure.core
       ring.adapter.jetty)
  (require [example-app.worker]))

(def worker-specs {:hard-worker {:fn example-app.worker/heavy-lifting, :queue "high-importance", :retry true}})
(def queue-specs {"high-importance" 25})

(defroutes app
  (GET "/" []
    (async-job-for :hard-worker [5000])
    "<h1>Bluecollar is workin' hard...</h1>"))

(defn -main [& args]
  (.addShutdownHook (Runtime/getRuntime)
      (Thread. #((bluecollar-teardown))))
  (bluecollar-setup queue-specs worker-specs)
  (run-jetty app {:port 8080 :join? true}))
