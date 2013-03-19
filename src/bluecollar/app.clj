(ns bluecollar.app
  (:use bluecollar.system))

(defrecord Bluecollar [config-impl msg-store-impl worker-impls]
  ISystem
  (config [this] config-impl)
  (message-storage [this] msg-store-impl)
  (workers [this] worker-impls))