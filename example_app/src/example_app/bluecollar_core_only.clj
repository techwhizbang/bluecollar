(ns example-app.bluecollar-core-only
  "An example where bluecollar.core is started by itself in it's own process.
   To start:
   => lein run -m example-app.bluecollar-core-only"

  (use bluecollar.core)
  (require [example-app.worker]))

(def worker-specs {:hard-worker {:fn example-app.worker/heavy-lifting, :queue "high-importance", :retry true}})
(def queue-specs {"high-importance" 25 "master" 25})

(defn -main [& args]
  (.addShutdownHook (Runtime/getRuntime)
      (Thread. #((bluecollar-shutdown))))
  (bluecollar-startup queue-specs worker-specs {:redis-timeout 0}))