;(ns bluecollar.config
;  (:use bluecollar.lifecycle)
;  (:require [clj-yaml.core :as yaml]))
;
;(defn- read-config [this]
;  (let [relative-path (:relative-path this)
;        env (:environment this)
;        config (yaml/parse-string (slurp (clojure.java.io/resource relative-path)))
;        env-config (env config)]
;    env-config))
;
;(def ^:private config-values (atom nil))
;
;(defn queues [] (:queues @config-values))
;(defn pool [] (:pool @config-values))
;
;(defrecord Configuration [relative-path environment]
;  ILifecycle
;  (start [this]
;    (reset! config-values (read-config this)))
;  (stop [this] (reset! config-values nil)))
;
