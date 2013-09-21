(ns bluecollar.keys-and-queues)

(def prefix
  ^{:doc "The prefix to all keys, lists, and data structures that pass through Redis.
          Feel free to change the name of this value if you see fit."}
  (atom "bluecollar"))

(def postfix
  ^{:doc "The postfix will get tacked onto the keys, lists, and data structures when it matters
          where the work is being performed."}
  (atom "default"))

(defn setup-prefix [prefix-name] (reset! prefix (or prefix-name "bluecollar")))

(defn setup-postfix [postfix-name] (reset! postfix (or postfix-name "default")))

(defn- prefix-key [key-name] (str @prefix ":" key-name))

(def ^{:doc "A map containing all the names of the keys used in bluecollar."
       :private true}
  key-registry (atom {}))

(def key-names #{"failure-retry-counter" "failure-total-counter"
                 "worker-runtimes" "success-total-counter"})

(defn worker-set-name [queue] (str @prefix ":" "workers-processing" ":" queue ":" @postfix))

(defn worker-key [queue uuid] (str (worker-set-name queue) ":" uuid))

(defn failure-retry-counter-key
  ^{:doc "The name of the key where the retry count is stored for a failed job."}
  [] (get @key-registry "failure-retry-counter"))

(defn failure-total-counter-key
  ^{:doc "The name of the key where the total count of failed jobs is stored."}
  [] (get @key-registry "failure-total-counter"))

(defn worker-runtimes-key
  ^{:doc "The name of the key the execution runtimes are stored for workers."}
  [worker-name] (str (get @key-registry "worker-runtimes") ":" worker-name))

(defn success-total-counter-key
  ^{:doc "The name of the key where the total count of successful jobs is stored."}
  [] (get @key-registry "success-total-counter"))

(defn- register-key [key-name]
  (swap! key-registry assoc key-name (prefix-key key-name)))

(defn register-keys []
  (reset! key-registry nil)
  (doseq [key-name key-names] (register-key key-name)))

(def
  ^{:doc "A map containing the original queue names as keys and the values
          containing modified versions of the queue names
          as they are used internally to bluecollar."
    :private true}
  queue-registry (atom {}))

(def master-queue-name "master")
(def master-processing-queue-name "master-processing")
(def processing-queue-name "processing")

(defn- register-queue [queue-name]
  (swap! queue-registry assoc queue-name (prefix-key (str "queues:" queue-name))))

(defn fetch-queue [queue-name]
  (let [q (get @queue-registry queue-name)]
    (if (nil? q)
      (do
        (register-queue queue-name)
        (fetch-queue queue-name))
      q)))

(defn register-queues [queues]
  (reset! queue-registry nil)
  (register-queue master-queue-name)
  (doseq [queue queues]
    (if-not (= queue "master")
      (register-queue queue))))
