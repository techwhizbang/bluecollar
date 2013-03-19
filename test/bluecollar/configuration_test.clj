(ns bluecollar.configuration-test
  (:use clojure.test)
  (:use bluecollar.config))

(deftest read-configuration-test
  (testing "reads the development configuration from the given path"
    (let [con (->Configuration "config/bluecollar.yml" :development)]
      (is (= {:queues '("uno" "dos" "tres")} (.start con)))
      )
    )

  (testing "reads the development configuration from the given path"
    (let [con (->Configuration "config/bluecollar.yml" :test)]
      (is (= {:queues '("quatro" "cinco" "seis")} (.start con)))
      )
    )
  )