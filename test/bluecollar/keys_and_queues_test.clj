(ns bluecollar.keys-and-queues-test
  (:use clojure.test)
  (:require [bluecollar.keys-and-queues :as kq]))

(def queues ["queue-1" "queue-2" "queue-3"])

(use-fixtures :each (fn [f]
  (kq/setup-prefix nil)
  (f)))

(deftest register-queues-test

  (testing "add queues to the registry"
    (kq/register-queues queues nil)
    (is (not (nil? (kq/fetch-queue "master"))))
    (is (not (nil? (kq/fetch-queue "processing"))))
    (doseq [queue queues]
      (is (not (nil? (kq/fetch-queue queue))))))

  (testing "resets the registry each time it is called"
    (kq/register-queues ["queue-55"] nil)
    (is (not (nil? (kq/fetch-queue "master"))))
    (is (not (nil? (kq/fetch-queue "processing"))))
    (is (not (nil? (kq/fetch-queue "queue-55")))))

  (testing "properly modifies the original queue name"
    (kq/register-queues queues nil)
    (is (= "bluecollar:queues:master" (kq/fetch-queue "master")))
    (is (= "bluecollar:queues:queue-1" (kq/fetch-queue "queue-1")))))

(deftest fetch-queue-test

  (testing "returns a known queue"
    (kq/register-queues queues nil)
    (is (= "bluecollar:queues:master" (kq/fetch-queue "master"))))

  (testing "registers a queue if unknown"
    (kq/register-queues queues nil)
    (is (= "bluecollar:queues:wonky-unregistered" (kq/fetch-queue "wonky-unregistered")))))

(deftest setup-prefix-test

  (testing "the default prefix"
    (kq/setup-prefix nil)
    (is (= "bluecollar" @kq/prefix)))

  (testing "the given prefix"
    (kq/setup-prefix "my-app")
    (is (= "my-app" @kq/prefix))))

(deftest register-keys-test
  (kq/register-keys)
  (is (= "bluecollar:failure-retry-counter" (kq/failure-retry-counter-key)))
  (is (= "bluecollar:failure-total-counter" (kq/failure-total-counter-key)))
  (is (= "bluecollar:worker-runtimes:hard-worker" (kq/worker-runtimes-key "hard-worker")))
  (is (= "bluecollar:success-total-counter" (kq/success-total-counter-key))))