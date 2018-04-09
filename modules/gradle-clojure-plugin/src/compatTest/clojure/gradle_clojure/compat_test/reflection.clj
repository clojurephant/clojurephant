(ns gradle-clojure.compat-test.reflection
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest reflection
  (gradle/with-project "BasicClojureProjectTest"
    (file/write-str (gradle/file "build.gradle") "compileClojure { options.aotCompile = true }\n" :append true)
    (testing "without reflection warnings enabled, nothing is output"
      (let [result (gradle/build "clean" "compileClojure")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (not (str/includes? (.getOutput result) "Reflection warning, basic_project/core.clj:11")))))
    (testing "with reflection warnings enabled"
      (file/write-str (gradle/file "build.gradle") "compileClojure { options.reflectionWarnings.enabled = true }\n" :append true)
      (testing "and asErrors false, warnings are only output"
        (let [result (gradle/build "clean" "compileClojure")]
          (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
          (is (str/includes? (.getOutput result) "Reflection warning, basic_project/core.clj:11"))))
      (testing "and asErrors true, warnings cause build to fail"
        (file/write-str (gradle/file "build.gradle") "compileClojure { options.reflectionWarnings.asErrors = true }\n" :append true)
        (let [result (gradle/build-and-fail "clean" "compileClojure")]
          (is (= TaskOutcome/FAILED (some-> result (.task ":compileClojure") .getOutcome)))
          (is (str/includes? (.getOutput result) "Reflection warning, basic_project/core.clj:11")))))))
