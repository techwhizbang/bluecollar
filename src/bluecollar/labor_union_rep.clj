(ns bluecollar.labor-union-rep)

(defn union-card-check
  "Before you get to work show me your union card!.
   It actually ensures that the given namespace has been loaded."
  [worker-ns]
  (if (not (contains? (loaded-libs) worker-ns))
    (require worker-ns)))