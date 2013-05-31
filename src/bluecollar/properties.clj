(ns bluecollar.properties)

(defn- set-application-properties []
  (doto (. System getProperties)
    (.setProperty "jetty.port" (or (get (System/getenv) "JETTY_PORT") "3100"))
    ; checks to see if the system property has been from startup otherwise sets the log path from ${user.dir}/log
    (.setProperty "bluecollar.log.dir" (or (System/getProperty "bluecollar.log.dir") (str (System/getProperty "user.dir") "/log")))
    ; add more properties here as needed
    ))

(set-application-properties)