(ns bluecollar.lifecycle)

(defprotocol Lifecycle
	(startup [_] "Start something up")
	(shutdown [_] "Shut something down"))