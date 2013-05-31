(defproject bluecollar "0.1.0-SNAPSHOT"
  :description "Bluecollar: an async worker library"
  :url "http://github.com/techwhizbang/bluecollar"
  :license {:name "MIT"
            :url "http://en.wikipedia.org/wiki/MIT_License"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-yaml "0.4.0"]
                 [clj-time "0.5.1"]
                 [com.taoensso/carmine "1.8.0"] ; Redis client
                 [cheshire "5.1.1"] ; JSON parsing

                 ;; Logging dependencies
                 [org.clojure/tools.logging "0.2.6"]
                 [log4j "1.2.17" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  :profiles {:dev {:dependencies [[org.clojars.runa/conjure "2.1.3"]]}})
