(ns bluecollar.system)

(defprotocol System
  (config [relative-path] "configuration for bluecollar workers")
  (storage-queue [storage] "implementation of queue storage, ie Redis, RabbitMQ")
  (workers [list-of-workers] "list of workers assigned to process tasks from the queues"))
