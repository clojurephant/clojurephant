(ns gradle-clojure.compat-test.clojure-test
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest clojure-test
  (testing "clojure.test failures cause the build to fail"
    (gradle/with-project "TestFailureFailsBuildTest"
      (let [result (gradle/build-and-fail "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileTestClojure") .getOutcome)))
        (is (= TaskOutcome/FAILED (some-> result (.task ":test") .getOutcome)))
        (is (str/includes? (.getOutput result) "3 tests completed, 2 failed")))))
  (testing "tests can be filtered by namespace via command line"
    (gradle/with-project "TestFailureFailsBuildTest"
      (let [result (gradle/build-and-fail "test" "--tests=basic-project.core-test2")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileTestClojure") .getOutcome)))
        (is (= TaskOutcome/FAILED (some-> result (.task ":test") .getOutcome)))
        (is (str/includes? (.getOutput result) "2 tests completed, 1 failed")))))
  (testing "tests can be filtered by test name via command line"
    (gradle/with-project "TestFailureFailsBuildTest"
      (let [result (gradle/build "test" "--tests=basic-project.core-test2.test-hello")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileTestClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":test") .getOutcome)))))))
