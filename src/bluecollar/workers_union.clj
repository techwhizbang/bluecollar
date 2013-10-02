(ns bluecollar.workers-union)

(defrecord UnionizedWorker [func queue retry])

(defn new-unionized-worker
  ([func queue retry] (->UnionizedWorker func queue retry))
  ([] (->UnionizedWorker nil nil nil)))

(def registered-workers
  "UnionizedWorker instances are stored here"
  (atom {}))

(defn clear-registered-workers
  ^{:doc "Resets any previously registered UnionizedWorker instances."}
  [] (reset! registered-workers {}))

(defn find-worker 
  ^{:doc "Find the UnionizedWorker based on the name it was given during
          setup in bluecollar.core or bluecollar.client."}
  [worker-name] (get @registered-workers worker-name))

(defn register-workers
  ^{:doc "Register all of your worker-definitions in one shot.
          The keys chosen will be the same keys used to enqueue
          a worker-definition."}
  [worker-definitions-map] (reset! registered-workers worker-definitions-map))

(defn register-worker
  ^{:doc "Register a single worker-definition with an unique key.
          The key chosen will be the same key used to enqueue
          a worker-definition."}
  [worker-key worker-definition] (swap! registered-workers assoc worker-key worker-definition))