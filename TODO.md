# beta release checklist

* "bluecollar" startup w/ Leiningen as a daemon
* A lifecycle hook for developers that want to extend "bluecollar"
    * Hook before a job runs
    * Hook after a job runs
* Recoverable against errors that occur in jobs.
* Reporting mechanism for when jobs fail.
    * daily email journal
* Lots of great logging
    * framework level logging
    * worker level logging
    * ability to specify a separate log for a worker
* Worker registry contains the following for each worker:
    * fn
    * queue
    * retry
* Scheduled jobs
    * Ability to "enqueue" a job to run in the future.
* Batch jobs
    * Group otherwise individual jobs into a grouping that can
      be identified in the UI and their overall progress can be
      tracked.
* Specify the Redis connection details in a YAML file
    * Add an optional namespace to keys in Redis
* A dashboard UI with the following:
    * Monitor number of workers busy
    * Monitor number of workers idle
    * Monitor the retry queue
    * Monitor current jobs and queues currently being processed
    * Monitor number of failed jobs
    * Live poll button for realtime updates
    * Web server startup w/ Leiningen command line
    * Might be cool to tinker with Pedestal...hmmm.
* Official domain name, logo, and a nicely styled static "homepage" hosted on Github.
    * Need to get a designer interested for the logo.
* Detailed usage and how-to in "README.md"
* Running on TravisCI
* upload it as a JAR on clojars.org
