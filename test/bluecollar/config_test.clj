(ns bluecollar.config-test
  (:use clojure.test
        bluecollar.config))

(deftest initializes-config-values-on-start-test
  (testing "initializes the configuration values for development on start"
    (let [_ (.start (->Configuration "config/bluecollar.yml" :development))]
      (is (=
        '({:name "uno", :priority 1, :pool 1}
          {:name "dos", :priority 9}
          {:name "tres", :priority 5}) (queues)))

      (is (= 10 (pool)))
      )
    )

  (testing "initializes the configuration values for test on start"
    (let [_ (.start (->Configuration "config/bluecollar.yml" :test))]
      (is (=
        '({:name "quatro", :priority 1, :pool 1}
          {:name "cinco", :priority 9}
          {:name "seis", :priority 5}) (queues)))

      (is (= 1 (pool)))
      )
    )
  )