(ns dev.clojurephant.compat-test.clojure-test
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [dev.clojurephant.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest clojure-test-fails
  (testing "clojure.test failures cause the build to fail"
    (gradle/with-project "TestFailureFailsBuildTest"
      (let [result (gradle/build-and-fail "check")]
        (gradle/verify-task-outcome result ":checkClojure" :success)
        (gradle/verify-task-outcome result ":compileTestClojure" :success)
        (gradle/verify-task-outcome result ":test" :failed)
        (is (str/includes? (.getOutput result) "3 tests completed, 2 failed"))))))

(deftest clojure-test-filter-namespace
  (testing "tests can be filtered by namespace via command line"
    (gradle/with-project "TestFailureFailsBuildTest"
      (let [result (gradle/build-and-fail "test" "--tests=basic_project.core_test2__init")]
        (gradle/verify-task-outcome result ":compileTestClojure" :success)
        (gradle/verify-task-outcome result ":test" :failed)
        (is (str/includes? (.getOutput result) "2 tests completed, 1 failed"))))))

(deftest clojure-test-filter-test-name
  (testing "tests can be filtered by test name via command line"
    (gradle/with-project "TestFailureFailsBuildTest"
      (let [result (gradle/build "test" "--tests=basic_project.core_test2__init.test-hello")]
        (gradle/verify-task-outcome result ":compileTestClojure" :success)
        (gradle/verify-task-outcome result ":test" :success)))))
