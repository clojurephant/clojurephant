(ns dev.clojurephant.compat-test.source-sets
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [dev.clojurephant.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest multiple-source-sets
  (testing "with multiple source sets, each gets its own set of tasks to compile the corresponding code"
    (gradle/with-project "MultipleSourceSetsTest"
      (let [result (gradle/build "clean" "check")]
        (gradle/verify-task-outcome result ":checkSs1Clojure" :success)
        (gradle/verify-task-outcome result ":checkSs2Clojure" :success)
        (gradle/verify-task-outcome result ":test" :success)))))

(deftest lein-build
  (testing "with Lein dir structure, tasks function as normal"
    (gradle/with-project "LeinClojureProjectTest"
      (let [result (gradle/build-and-fail "check")]
        (gradle/verify-task-outcome result ":checkClojure" :success)
        (gradle/verify-task-outcome result ":compileTestClojure" :success)
        (gradle/verify-task-outcome result ":test" :failed)
        (is (str/includes? (.getOutput result) "2 tests completed, 1 failed"))))))
