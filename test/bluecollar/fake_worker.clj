(ns bluecollar.fake-worker)

(def perform-called (atom false))
(def fake-worker-failures (atom 0))
(def cnt-me (atom 0))

;TODO call swap! and inc the value of perform-called
(defn perform [arg1 arg2]
  (reset! perform-called true))

(defn counting []
  (swap! cnt-me inc))

(defn explode []
  (swap! fake-worker-failures inc)
  (throw (RuntimeException. (str "Exploded " @fake-worker-failures " time(s)."))))