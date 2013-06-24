# beta release checklist
* Give an option during startup to customize the default "bluecollar" namespace.
* Need to define a client setup that establishes a Redis connection pointing to the correct
  namespace with the same worker definitions as core.
* Re-coverability - if bluecollar terminates ungracefully while processing jobs 
  and leaves jobs in the "processing" queue; recover the orphaned jobs and retry them.
    * Each instance of bluecollar should get its own processing queue in Redis.
    * Interesting problem to solve when distributed, who's in charge of determining what needs recovery?
    * To be truly distributed it shouldn't be limited to just the process or server that terminated     ungracefully to recover
    * Maybe initially it is said that the bluecollar instance that terminated ungracefully is in charge or recovering its own jobs.
    * On boot up it should check its own processing queue for leftover work.
* Calculate the average run time statistics for each worker
* Calculate the entire total of successful jobs processed.
* Calculate the entire total of failed jobs processed.
* Indicate server hostname a job plan is being executed. (in progress)
* Detailed usage and how-to in "README.md"
    * "bluecollar" startup instructions w/ Leiningen as a daemon, look at lein-daemon
    * include a canonical example application using bluecollar
* Official domain name, logo, and a nicely styled static "homepage" hosted on Github.
    * Need to get a designer interested for the logo.

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
