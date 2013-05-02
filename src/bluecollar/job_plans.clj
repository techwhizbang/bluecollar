(ns bluecollar.job-plans
  (:require [cheshire.core :as json]))

(defn as-map [ns arg-vec]
  {"ns" (symbol (str ns "/perform")) "args" arg-vec})

(defn as-list [plan-as-map]
  (list (get plan-as-map "ns") (get plan-as-map "args")))

(defn for-worker [plan-as-map]
  (let [evalutables (as-list plan-as-map)]
    (fn [] (eval evalutables))))

(defn as-json [ns arg-vec]
  (json/generate-string (as-map ns arg-vec)))

(defn from-json [plan-as-json]
  (let [parsed-map (json/parse-string plan-as-json)]
    {"ns" (symbol (get parsed-map "ns")) "args" (get parsed-map "args")}
    ))