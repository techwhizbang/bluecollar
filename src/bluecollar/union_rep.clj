(ns bluecollar.union-rep)

(defrecord WorkerDefinition [func queue retry])

(defn new-worker-definition 
  ([func queue retry] (->WorkerDefinition func queue retry))
  ([queue] (->WorkerDefinition nil queue nil)))

(def registered-workers
  "worker-definitions are stored here"
  (atom {}))

(defn find-worker [worker-name]
  (get @registered-workers worker-name))

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
  [worker-key worker-definition]
  (swap! registered-workers assoc worker-key worker-definition))