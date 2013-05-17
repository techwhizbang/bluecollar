(ns bluecollar.fake-worker)

(def perform-called (atom false))

(defn perform [arg1 arg2]
  (reset! perform-called true))

(defn explode []
	(throw (RuntimeException. "this is intended to explode.")))