(ns bluecollar.core
	"The core namespace for the bluecollar library.

	In order to start the bluecollar environment, create two hash maps containing
	the queue specifications and the other containing the worker specifications.

	The queue specifications is a hash map.
	Each keyword will be used as a queue name.
	The value of each keyword will determine how large thread pool representing that queue should be.
	It can contain any number of arbitrarily named queues.

	In this example there are 3 queue specifications:

	=> {:high-importance 10 :medium-importance 5 :catch-all 5}

	The worker specifications is also a hash map.
	Each keyword will represent a worker and how it is referenced to send it jobs.
	The value for each worker specification is a hash map containing that must contain 3 main things:
		1.) The queue it should be placed on in order to be processed.
		2.) The namespace and function it should execute when being processed.
		3.) The ability to retry if the job given to the worker results in an exception.

	In this example there are 2 worker specifications:

	=> {:worker-one {:fn clojure.core/+, :queue :high-importance, :retry true}
   	  :worker-two {:fn nick.zalabak/blog, :queue :catch-all, :retry false}}

  In order to start bluecollar:

  => (use 'bluecollar.core)
  => (def queue-specs {:high-importance 10 :medium-importance 5 :catch-all 5})
  => (def worker-specs {:worker-one {:fn clojure.core/+, :queue :high-importance, :retry true}
   	  									:worker-two {:fn nick.zalabak/blog, :queue :catch-all, :retry false}})
  => (bluecollar-startup queue-specs worker-specs)

  In order to safely shut down bluecollar:

  => (bluecollar-shutdown)

	"
	(:use bluecollar.lifecycle
				bluecollar.properties)
	(:require [bluecollar.job-sites :as job-site]
		  			[bluecollar.union-rep :as union-rep]
		  			[clojure.tools.logging :as logger]))

(def job-sites (atom []))

(defn bluecollar-startup
	"Start up the bluecollar environment by passing it the specifications for both the
	 queues and workers."
	[queue-specs worker-specs]
	(logger/info "Bluecollar is starting...")
	(doseq [[worker-name worker-defn] worker-specs]
		(union-rep/register-worker worker-name (struct union-rep/worker-definition
    	                                  					 (:fn worker-defn)
      	                                					 (:queue worker-defn)
        	                              					 (:retry worker-defn))))
	(doseq [[queue-name pool-size] queue-specs]
		(swap! job-sites conj (job-site/new-job-site queue-name pool-size)))
	(doseq [site @job-sites] (startup site)))

(defn bluecollar-shutdown
	"Shut down the bluecollar environment"
	[]
	(logger/info "Bluecollar is shutting down...")
	(if-not (empty? @job-sites)
		(do
			(doseq [site @job-sites] (shutdown site))
			(reset! job-sites []))))