# bluecollar

[![Build Status](https://travis-ci.org/techwhizbang/bluecollar.png)](https://travis-ci.org/techwhizbang/bluecollar)
 
<img src="https://raw.github.com/techwhizbang/bluecollar/master/creative/bluecollar_dude.png"
 alt="Bluecollar logo" title="Bluecollar Dude" align="right" height="388" width="300"/>

## What is it?

The aim of `bluecollar` is to provide a simple yet full featured distributed background processing solution written in Clojure.  
  
<br/><br/><br/>
      
      

## Who is it for?  

Any Clojure application that is tasked with lots of time and/or resource intensive operations that require
reliability.

## What does it do exactly?

`bluecollar` simply runs alongside or within any current Clojure application. It takes any set of 
functions that exist in your application and can run them asynchronously, reliably, and of course distribute the work to any number of `bluecollar` server instances.

## Is it production ready?
Yes! I'm currently running `bluecollar` in production for a mission critical application that processes <b>millions of records every day</b>. Yes, millions.

## Installation

`bluecollar` artifacts are [released to Clojars](https://clojars.org/bluecollar/bluecollar).

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
[bluecollar/bluecollar "1.0.0"]
```

With Maven:

``` xml
<dependency>
  <groupId>bluecollar</groupId>
  <artifactId>bluecollar</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Usage

Take a look at the [Quick Start](https://github.com/techwhizbang/bluecollar/wiki/Quick-Start) 
or peruse the [Wiki](https://github.com/techwhizbang/bluecollar/wiki) to begin using `bluecollar`.


## Example Application

Have a peek at the [example_app](https://github.com/techwhizbang/bluecollar/tree/master/example_app) if you'd like to
see a more concrete example of how to use `bluecollar`.

## Thanks

A special thanks to [Ben Day](https://github.com/benjiuday) for creating the `bluecollar` logo.

## License

Copyright (c) 2013 Nick Zalabak

Distributed under the Eclipse Public License.
http://www.eclipse.org/legal/epl-v10.html
