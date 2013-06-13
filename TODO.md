# beta release checklist

* Indicate server hostname a job plan is being executed.
* Add an optional namespace to keys everything pushed to Redis
* "bluecollar" startup w/ Leiningen as a daemon, look at lein-daemon
* Reporting mechanism for when jobs fail.
    * daily email journal
* Lots of great logging
    * ability to specify a separate log for a superintendent
* Re-coverability
    * If the process terminates ungracefully and leaves jobs in the
      "processing" queue; recover those jobs and retry them.
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
* Official domain name, logo, and a nicely styled static "homepage" hosted on Github.
    * Need to get a designer interested for the logo.
* Detailed usage and how-to in "README.md"
* Running on TravisCI
* upload it as a JAR on clojars.org