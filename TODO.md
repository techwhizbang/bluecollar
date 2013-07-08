# beta release checklist
* Calculate the average run time statistics for each worker
    * "worker-avg-runtime" [20s, 25s, 56s...]
* Indicate server hostname a job plan is being executed.
* Detailed usage and how-to in "README.md"
    * "bluecollar" startup instructions w/ Leiningen as a daemon, look at lein-daemon
    * include a canonical example application using bluecollar
    * wiki pages  
* Official domain name, logo, and a nicely styled static "homepage" hosted on Github.
    * Need to get a designer interested for the logo.
* Get bluecollar added to http://redis.io/clients

# future releases
* A dashboard UI with the following:
    * Monitor number of workers busy
    * Monitor number of workers idle
    * Monitor the retry queue
    * Monitor current jobs and queues currently being processed
    * Monitor number of failed jobs
    * Live poll button for realtime updates
    * Web server startup w/ Leiningen command line
    * Might be cool to tinker with Pedestal...hmmm.
* Batch jobs
    * Group otherwise individual jobs into a grouping that can
      be identified in the UI and their overall progress can be
      tracked.
* Configurable to send emails through the servers local sendmail
    * daily email journal
* Distributed re-coverability - if the process terminates ungracefully while processing jobs 
  and leaves jobs in the "processing" queue; recover the orphaned jobs and retry them.
    * Need an arbiter...a basic process in charge of making decisions on behalf of a cluster
      of distributed bluecollar instances.
    * The arbiter gets a list of bluecollar instances on what servers and what ports.
    * The arbiter pings the bluecollar instances occassionally on those ports to ensure they're still alive.
    * If an instance becomes unresponsive, then check the processing queue assigned to that instance and see if
      any jobs are left there, if they are then remove from processing and add back into the named queue.
