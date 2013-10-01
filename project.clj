(defproject bluecollar/bluecollar "1.0.1-SNAPSHOT"

  :description "Bluecollar: a simple yet full featured distributed background processing solution written in Clojure."
  :url "http://github.com/techwhizbang/bluecollar"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-time "0.5.1"]
                 [com.taoensso/carmine "2.2.0"] ; Redis client
                 [cheshire "5.1.1"] ; JSON parsing

                 ;; Logging dependencies
                 [org.clojure/tools.logging "0.2.6"]
                 [log4j "1.2.17" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]

  :profiles {:dev {:dependencies [[org.clojars.runa/conjure "2.1.3"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}}

  :aliases {"test-all" ["with-profile" "dev,default:dev,1.4,default:dev,1.5,default" "test"]})
