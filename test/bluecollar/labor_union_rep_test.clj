(ns bluecollar.labor-union-rep-test
  (:use clojure.test
        bluecollar.test-helper)
  ; explicitly not requiring bluecollar.fake-worker to ensure the labor-rep is doing
  ; it's job
  (:require [bluecollar.labor-union-rep :as labor-rep]))

(deftest union-card-check-test
  (testing "if not alreay loaded include the given namespace"
    (let [_ (labor-rep/union-card-check 'bluecollar.fake-worker)]
      (is (true? (contains? (loaded-libs) 'bluecollar.fake-worker)))
      (is (true? (eval (list 'bluecollar.fake-worker/perform 1 2)))))
    ))