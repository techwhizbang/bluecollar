(ns bluecollar.lifecycle)

(defprotocol ILifecycle
  (start [this] "start a component attached to a system")
  (stop [this] "stop a component attached to a system"))
