(ns bluecollar.lifecycle)

(defprotocol ILifecycle
  "A protocol for componentry that is startable and stoppable in an ISystem"
  (start [this] "start a component that belongs to an ISystem")
  (stop [this] "stop a component that belongs to an ISystem"))
