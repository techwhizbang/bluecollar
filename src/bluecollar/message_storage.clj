(ns bluecollar.message-storage)

(defprotocol IMessageStorage
  "A protocol for message store implementations to follow"
  (flushdb [_] "flushes all the keys from the current db")
  (push [_ queue-name value] "add a message to the end of the given queue")
  (consume [_ queue-name] "consume the next message from the given queue")
  (connection-pool [_ redis-config] "return connection pool for the message store"))
