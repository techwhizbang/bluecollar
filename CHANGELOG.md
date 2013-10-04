# bluecollar changelog

## 1.0.1 - Coming soon

* Upgraded to carmine 2.2.3 from 1.8.0 and removed usages of deprecated API
* Improving the MasterQueue to not use excessive Redis connections by maintaining a singular pool 
  that is sized appropriately to the number of workers it has, just as a Foreman does. 
* Fixed a defect in bluecollar-client-startup where the :redis-key-postfix option was being ignored.

## 1.0.0 - Sept 28, 2013

* Finally releasing 1.0.0 after running in a production environment for over 2 weeks and working
  out the kinks in previous beta releases.

## 1.0.0-beta8 - Sept 21, 2013

* Bug fix release addressing Redis connection issues dropping and/or hanging because of the use of the
  redis/with-transaction fn (ie MULTI/EXEC) during startup. The problem has been resolved by removing usage
  of redis/with-transaction until I can re-write the Redis connection management.

## 1.0.0-beta7 - Sept 20, 2013

* Bug fix release addressing issues arising from trying to remove LREM and replace with SETS and KEYS

## 1.0.0-beta6 - Sept 17, 2013

* Bug fix release addressing issues arising from trying to remove LREM and replace with SETS and KEYS

## 1.0.0-beta5 - Sept 17, 2013

* Addressing a Redis CPU load problem due to the use of LREM when the processing queues were getting extremely large. The way work is "polled" has been improved to match exactly the number of pooled workers allocated which in turn makes the processing queue only as large as the worker pool. 

## 1.0.0-beta4 - Aug 22, 2013

* Implemented a master queue where all the job plan messages go initially, then they are distributed
to their respective queues from there. This greatly simplifies and shields the client from knowing
to much about what the workers are doing or what queue they work on. Now any bluecollar client can 
push job plans to the master queue and the bluecollar.core server side takes care of the rest. 

## 1.0.0-beta3 - Aug 13, 2013

* Fixed bug where the user defined queue names are not being prefixed properly ie. queue-blah should be "bluecollar:queue-blah"
* Fixed JETTY_PORT bug that was overriding a previously set System property defining the Jetty port

## 1.0.0-beta2 - July 16, 2013

* fixing a little formatting defect showing
  up with two colons in the worker runtimes
  list instead of one since the worker name
  is a keyword

* re-naming the union-rep ns to workers-union

* re-naming the defrecord WorkerDefinition to UnionizedWorker

## 1.0.0-beta1 - July 13, 2013

* First publicly available artifact containing the foundation of bluecollar.



