(ns bluecollar.core
	(require [bluecollar.job-sites :as job-sites]))

(def ^:private bosses (atom {}))

; (defmacro bluecollar-startup [queue-specs worker-specs]
; 	(doseq [queue queue-specs]
; 		))


; ; (bluecollar-startup
; ; 	{:queue-one 5 :queue-two 10
; ; 	{:worker-one {} :worker-two {}})

; ; (bluecollar-shutdown)