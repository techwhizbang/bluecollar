(ns bluecollar.app-test
  (:use clojure.test
        bluecollar.app))

(deftest initializing-bluecollar-app-test

  (testing "can set and retrieve the configuration"
    (let [config :config
          bc-app (->Bluecollar config nil nil)]
      (is (= config (.config bc-app))))
    )

  (testing "can set and retrieve the message storage"
    (let [msg-storage :foo
          bc-app (->Bluecollar nil msg-storage nil)]
      (is (= msg-storage (.message-storage bc-app))))
    )

  (testing "can set and retrieve the workers"
    (let [workers '(one two three)
          bc-app (->Bluecollar nil nil workers)]
      (is (= workers (.workers bc-app)))))
  )