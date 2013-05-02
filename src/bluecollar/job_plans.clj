(ns bluecollar.job-plans
  (:require [cheshire.core :as json]))

(defn as-map [ns arg-vec]
  {"ns" (symbol ns) "args" arg-vec})

(defn as-list [plan-as-map]
  (flatten (list (get plan-as-map "ns") (get plan-as-map "args"))))

(defn for-worker
  "The 'job plan' for a worker is unique because it attaches the perform
   function that should be called from the given namespace. It returns
   a wrapper function that can be used by a 'worker' which ultimately
   evaluates a list containing the namespace/fn plus it's arugments."
  [plan-as-map]
  (let [plain-ns (get plan-as-map "ns")
        ns-with-perform (symbol (str plain-ns "/perform"))
        plan-with-perform (assoc plan-as-map "ns" ns-with-perform)
        evalutables (as-list plan-with-perform)]
    (fn [] (eval evalutables))))

(defn as-json [ns arg-vec]
  (json/generate-string (as-map ns arg-vec)))

(defn from-json [plan-as-json]
  (let [parsed-map (json/parse-string plan-as-json)]
    {"ns" (symbol (get parsed-map "ns")) "args" (get parsed-map "args")}
    ))