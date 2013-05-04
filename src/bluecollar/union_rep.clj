(ns bluecollar.union-rep)


; instead of the meta-programming namespace approach,
; register the worker-fns here with a key for lookup
; and the function to be called.
; then use (apply fn args) for dispatch
; example
; (def registry {:important-job bluecollar.fake-worker/perform})
; (apply (get registry :important-job) [1 2])
; (fn [] (apply (get registry :important-job) [1 2]))
(def worker-registry
  (atom {}))

(defn union-card-check
  "Before you get to work show me your union card!.
   It actually ensures that the given namespace has been loaded."
  [worker-ns]
  (if (not (contains? (loaded-libs) worker-ns))
    (require worker-ns)))