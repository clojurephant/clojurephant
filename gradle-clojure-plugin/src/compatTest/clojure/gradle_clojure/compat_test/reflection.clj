(ns gradle-clojure.compat-test.reflection
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest reflection-no-output-no-error
  (gradle/with-project "BasicClojureProjectTest"
    (testing "with reflection :silent, nothing is output"
      (let [result (gradle/build "clean" "checkClojure")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":checkClojure") .getOutcome)))
        (is (not (str/includes? (.getOutput result) "Reflection warning, basic_project/core.clj:12")))))))

(deftest reflection-output-no-error
  (gradle/with-project "BasicClojureProjectTest"
    (testing "with reflection :warn, warnings are only output"
      (file/write-str (gradle/file "build.gradle") "checkClojure { reflection = 'warn' }\n" :append true)
      (let [result (gradle/build "clean" "checkClojure")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":checkClojure") .getOutcome)))
        (is (str/includes? (.getOutput result) "Reflection warning, basic_project/core.clj:12"))))))

(deftest reflection-output-error
  (gradle/with-project "BasicClojureProjectTest"
    (testing "with reflection :fail, warnings in the project cause the build to fail"
      (file/write-str (gradle/file "build.gradle") "checkClojure { reflection = 'fail' }\n" :append true)
      (let [result (gradle/build-and-fail "clean" "checkClojure")]
        (is (= TaskOutcome/FAILED (some-> result (.task ":checkClojure") .getOutcome)))
        (is (str/includes? (.getOutput result) "Reflection warning, basic_project/core.clj:12"))))))
