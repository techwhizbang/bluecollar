(ns bluecollar.test-helper
  (:use clojure.test)
  (:require [bluecollar.redis :as redis]))

(def redis-test-settings {:host "127.0.0.1", :port 6379, :db 0, :timeout 5000})
(def testing-queue-name "testing-queue-name")

(def uuid-regex #"\p{XDigit}{8}-\p{XDigit}{4}-\p{XDigit}{4}-\p{XDigit}{4}-\p{XDigit}{12}")

(defn redis-setup []
  (do
    (redis/startup redis-test-settings)
    (redis/flushdb)))

(defmacro use-redis-test-setup []
  `(use-fixtures :each (fn [f#] (redis-setup) (f#))))
