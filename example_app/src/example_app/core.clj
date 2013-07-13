(ns example-app.core
  "This is an example application where bluecollar.core and bluecollar.client
   are side by side in a single process. In other words, you are sending jobs to be done
   and processing them in the same process.

   In some situtations where you're processing intensive jobs you would choose to run one
   or many bluecollar.core process on many servers separate from your web or service layer."
  (use bluecollar.core
       bluecollar.client
       compojure.core
       ring.adapter.jetty))

(defn heavy-lifting [sleep-millis]
  (Thread/sleep sleep-millis)
  (str "That was hard work!!!"))

(def worker-specs {:hard-worker {:fn example-app.core/heavy-lifting, :queue "high-importance", :retry true}})
(def queue-specs {"high-importance" 10})

(defroutes app
  (GET "/" []
    (async-job-for :hard-worker [5000])
    "<h1>Bluecollar is workin' hard...</h1>"))

(defn -main [& args]
  (.addShutdownHook (Runtime/getRuntime)
      (Thread. #((bluecollar-teardown))))
  (bluecollar-setup queue-specs worker-specs)
  (run-jetty app {:port 8080 :join? true}))
