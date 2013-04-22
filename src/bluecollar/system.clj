(ns bluecollar.system)

(defprotocol ISystem
  (config [config-impl] "configuration for bluecollar")
  (message-storage [msg-store-impl] "message storage adapter")
  (workers [worker-impls] "list of workers assigned to process tasks from the queues"))
