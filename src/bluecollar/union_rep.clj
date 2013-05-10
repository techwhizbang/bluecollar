(ns bluecollar.union-rep)

(defstruct worker-definition "fn" "queue" "retry")

(def registered-workers
  "worker-definitions are stored here"
  (atom {}))

(defn register-workers
  "Register all of your worker-definitions in one shot.
   The keys chosen will be the same keys used to enqueue
   a worker-definition."
  [worker-definitions-map]
  (reset! registered-workers worker-definitions-map))

(defn register-worker
  "Register a single worker-definition with an unique key.
   The key chosen will be the same key used to enqueue
   a worker-definition."
  [key worker-definition]
  (swap! registered-workers assoc key worker-definition))