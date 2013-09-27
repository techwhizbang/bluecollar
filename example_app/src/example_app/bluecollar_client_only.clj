(ns example-app.bluecollar-client-only
  " To start:
   => lein run -m example-app.bluecollar-client-only"
  (use bluecollar.client
       compojure.core
       ring.adapter.jetty))

(def worker-specs [:hard-worker])

(defroutes app
  (GET "/" []
    (async-job-for :hard-worker [500])
    "<h1>Bluecollar is workin' hard...</h1>"))

(defn -main [& args]
  (bluecollar-client-startup worker-specs)
  (run-jetty app {:port 8080 :join? true}))