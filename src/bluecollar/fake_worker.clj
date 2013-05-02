(ns bluecollar.fake-worker)

(def called-me (atom false))

(defn perform [arg1 arg2]
  (reset! called-me true))