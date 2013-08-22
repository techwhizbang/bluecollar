# bluecollar changelog

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



