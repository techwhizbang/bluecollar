(ns bluecollar.config
  (:use bluecollar.lifecycle)
  (:require [clj-yaml.core :as yaml]))

(defn- read-config [this]
  (let [relative-path (:relative-path this)
        env (:environment this)
        config (yaml/parse-string (slurp (clojure.java.io/resource relative-path)))
        env-config (env config)]
    env-config
    )
  )

(defrecord Configuration [relative-path environment] ILifecycle

  (start [this] (read-config this))
  (stop [this] nil))

