# bluecollar

[![Build Status](https://travis-ci.org/techwhizbang/bluecollar.png)](https://travis-ci.org/techwhizbang/bluecollar)

Great news, `bluecollar` is getting close to "beta" completion.
Please stay tuned for its release!

## What is it?

The aim of `bluecollar` is to provide a simple yet fully featured distributed background processing solution written in Clojure.

## Who is it for?

Any Clojure application that is tasked with lots of time and/or resource intensive operations. 

## What does it do exactly?

`bluecollar` simply runs alongside or within any current Clojure application. It takes any set of 
functions that exist in your application and can run them asynchronously, reliably, and of course distribute the work to any number of `bluecollar` server instances.

## Installation

`bluecollar` artifacts are [released to Clojars](https://clojars.org/techwhizbang/bluecollar).

If you are using Maven, add the following repository definition to your `pom.xml`:

``` xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

### The Most Recent Release

With Leiningen:

``` clj
Coming soon!
```

With Maven:

``` xml
<dependency>
  <groupId>techwhizbang</groupId>
  <artifactId>bluecollar</artifactId>
  <version>Coming Soon!</version>
</dependency>
```

## Usage

### bluecollar.core

`bluecollar.core` is intended to act as the server side component where the actual heavy duty
processing occurs. Insert `bluecollar.core` to the area of your application 
where bootstrapping and start up occurs.

In order to start `bluecollar.core`:
```clj
(use 'bluecollar.core)

; queue-specs represents the name of the queues and how many "worker" threads are assigned to each
(def queue-specs {"high-importance" 10 "medium-importance" 5 "catch-all" 5})

; worker-specs represents a mapping of workers and the functions they are assigned to execute,
; the queue they gather work from, and if on failure whether they should retry
(def worker-specs {:worker-one {:fn clojure.core/+, :queue "high-importance", :retry true}
                   :worker-two {:fn nick.zalabak/blog, :queue "catch-all", :retry false}})

; redis-specs represents the details of how to connect to Redis
(def redis-specs {:redis-hostname "redis-master.my-awesome-app.com",
                  :redis-port 1234,
                  :redis-db 6,
                  :redis-timeout 6000})  

(bluecollar-setup queue-specs worker-specs redis-specs)
```

In order to safely teardown `bluecollar.core`:
```clj
(use 'bluecollar.core)
(bluecollar-teardown)
```

### bluecollar.client

`bluecollar.client` is intended to act as a lightweight interface to `bluecollar.core`. It basically pushes job messages to Redis that are picked up and processed by `bluecollar.core`.


## License

Copyright (c) 2013 Nick Zalabak

Distributed under the Eclipse Public License.
http://www.eclipse.org/legal/epl-v10.html