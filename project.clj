(defproject bluecollar "0.1.0-SNAPSHOT"
  :description "Bluecollar: an async worker library"
  :url "http://github.com/techwhizbang/bluecollar"
  :license {:name "MIT"
            :url "http://en.wikipedia.org/wiki/MIT_License"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-yaml "0.4.0"]
                 [clj-time "0.5.0"]
                 [com.taoensso/carmine "1.6.0"]
                 [cheshire "5.1.1"]]
  :profiles {:dev {:dependencies [[org.clojars.runa/conjure "2.1.3"]]}})
