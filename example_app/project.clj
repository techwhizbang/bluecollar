(defproject example_app "0.1.0-SNAPSHOT"
  :description "An example using bluecollar"
  :url "http://github.com/techwhizbang/bluecollar"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [bluecollar/bluecollar "1.0.0"]
                 [compojure "1.1.5"]
                 [ring "1.2.0"]]
  :main example-app.bluecollar-core-and-client)