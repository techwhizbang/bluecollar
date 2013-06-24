(ns bluecollar.job-sites-test
  (:use clojure.test
    bluecollar.lifecycle
    bluecollar.client
    bluecollar.test-helper)
  (:require [bluecollar.redis :as redis]
            [bluecollar.fake-worker]
            [bluecollar.union-rep :as union-rep]
            [bluecollar.job-sites :as job-site]
            [bluecollar.job-plans :as plan]
            [cheshire.core :as json]))

(use-fixtures :each (fn [f]
  (redis/startup redis-test-settings)
  (redis/flushdb)
  (reset! bluecollar.fake-worker/perform-called false)
  (reset! bluecollar.fake-worker/fake-worker-failures 0)
  (f)))

(deftest job-site-with-passing-job-plan-test
  (testing "that the job plan is passed to the foreman and the foreman dispatches work"
    (let [workers {:hard-worker (struct union-rep/worker-definition 
                                        bluecollar.fake-worker/perform
                                        testing-queue-name
                                        false)}
          _ (union-rep/register-workers workers)
          a-job-site (job-site/new-job-site testing-queue-name 5)
          _ (startup a-job-site)
          _ (Thread/sleep 1000)
          _ (async-job-for :hard-worker [3 2])
          _ (Thread/sleep 3000) ; wait for the job plan to be completed
          _ (shutdown a-job-site)
          _ (Thread/sleep 2000) ; wait for shutdown to complete
          in-processing-vals (redis/lrange (redis/processing-queue) 0 0)]
      (is (true? (deref bluecollar.fake-worker/perform-called)))
      (is (empty? in-processing-vals))
      )))

(deftest job-site-with-failing-job-plan-test
  (testing "a failing worker is retried the maximum number of times without crashing the process"
    (let [_ (reset! plan/delay-base 1)
          workers {:failing-worker (struct union-rep/worker-definition 
                                           bluecollar.fake-worker/explode
                                           testing-queue-name
                                           true)}
          _ (union-rep/register-workers workers)
          a-job-site (job-site/new-job-site testing-queue-name 5)
          _ (startup a-job-site)
          _ (Thread/sleep 1000) 
          _ (async-job-for :failing-worker [])
          _ (Thread/sleep 3000) ; wait for all of the job plans to complete
          _ (shutdown a-job-site)
          _ (Thread/sleep 2000)] ; wait for shutdown to complete
      (is (= 25 (deref bluecollar.fake-worker/fake-worker-failures)))
      )))

(deftest multi-job-sites-test
  (testing "a failing worker is retried the maximum number of times without crashing the process"
    (let [_ (reset! plan/delay-base 1)
          workers {:worker-one (struct union-rep/worker-definition 
                                       bluecollar.fake-worker/counting
                                       "queue 1"
                                       true)
                   :worker-two (struct union-rep/worker-definition 
                                       bluecollar.fake-worker/counting
                                       "queue 2"
                                       true)}
          _ (union-rep/register-workers workers)
          job-site-1 (job-site/new-job-site "queue 1" 5)
          job-site-2 (job-site/new-job-site "queue 2" 5)
          _ (startup job-site-1)
          _ (startup job-site-2)
          _ (Thread/sleep 1000) 
          _ (async-job-for :worker-one [])
          _ (async-job-for :worker-two [])
          _ (Thread/sleep 3000) ; wait for all of the job plans to complete
          _ (shutdown job-site-1)
          _ (shutdown job-site-2)
          _ (Thread/sleep 2000)] ; wait for shutdown to complete
      (is (= 2 (deref bluecollar.fake-worker/cnt-me)))
      )))

(deftest new-job-site-test
  (let [job-site (job-site/new-job-site "the name" 5)]
    (testing "returns a new JobSite"
      (is (instance? bluecollar.job_sites.JobSite job-site)))

    (testing "has a foreman"
      (is (instance? bluecollar.foreman.Foreman (:foreman job-site))))

    (testing "has a site-name"
      (is (= "the name" (:site-name job-site))))))