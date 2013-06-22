# beta release checklist
* Give an option during startup to override the default "bluecollar" namespace.
* Re-coverability - if the process terminates ungracefully while processing jobs 
  and leaves jobs in the "processing" queue; recover the orphaned jobs and retry them.
    * Interesting problem to solve if there are workers processing the same queue
      that are distributed across multiple servers...who's in charge in determining what needs recovery?
    * To be truly distributed it shouldn't be limited to just the process or server that terminated ungracefully
      to recover
    * Maybe initially it is said that processing duplicate queues across multiple processes or servers
      is not recommended until a more robust solution
    * Maybe it is time based, if left in processing for more than an hour and if a new
      bluecollar instance is booted up or is restarted first check the processing queue
      for stale jobs and retry them...this however is problematic for long running jobs!
    * Maybe as part of the JobPlan JSON pushed into the processing queue it begins to include the
      server hostname it is/was being processed on, and if left in the processing queue for over an hour, 
      a bluecollar instance known as an "arbiter" can ping that bluecollar instance based on the server hostname 
      on a particular port to see if it is still alive... If it's still alive, and it responds assume the best,
      if it doesn't move it from the processing and place it back into the named queue it came from.
* Calculate the average run time statistics for each worker
* Calculate the entire total of successful jobs processed.
* Calculate the entire total of failed jobs processed.
* Indicate server hostname a job plan is being executed. (in progress)
* Detailed usage and how-to in "README.md"
    * "bluecollar" startup instructions w/ Leiningen as a daemon, look at lein-daemon
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
