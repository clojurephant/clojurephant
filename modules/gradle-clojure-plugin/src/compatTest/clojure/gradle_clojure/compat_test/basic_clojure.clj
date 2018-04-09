(ns gradle-clojure.compat-test.basic-clojure
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest basic-build
  (testing "without AOT compile, only source files are copied to the output directory"
    (gradle/with-project "BasicClojureProjectTest"
      (let [result (gradle/build "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileTestClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":test") .getOutcome)))
        (gradle/verify-compilation-without-aot "src/main/clojure" "build/classes/clojure/main"))))

  (testing "with AOT compile, only class files are copied to the output directory"
    (gradle/with-project "BasicClojureProjectTest"
      (let [result (gradle/build "-DaotCompile=true" "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileTestClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":test") .getOutcome)))
        (gradle/verify-compilation-with-aot "src/main/clojure" "build/classes/clojure/main"))))

  (testing "stale output files are cleaned"
    (gradle/with-project "BasicClojureProjectTest"
      (let [result (gradle/build "classes")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (gradle/verify-compilation-without-aot "src/main/clojure" "build/classes/clojure/main"))

      (file/delete (gradle/file "src/main/clojure/basic_project/utils.clj"))
      (let [result (gradle/build "compileClojure")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (gradle/verify-compilation-without-aot "src/main/clojure" "build/classes/clojure/main")))))

(deftest multiple-source-sets
  (testing "with multiple source sets, each gets its own set of tasks to compile the corresponding code"
    (gradle/with-project "MultipleSourceSetsTest"
      (let [result (gradle/build "clean" "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileSs1Clojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileSs2Clojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileTestClojure") .getOutcome)))
        (is (= (gradle/file-tree "src/ss1/clojure") (gradle/file-tree "build/classes/clojure/ss1")))
        (gradle/verify-compilation-with-aot "src/ss2/clojure" "build/classes/clojure/ss2")))))

(deftest lein-build
  (testing "with Lein dir structure, tasks function as normal"
    (gradle/with-project "LeinClojureProjectTest"
      (let [result (gradle/build-and-fail "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileTestClojure") .getOutcome)))
        (is (= TaskOutcome/FAILED (some-> result (.task ":test") .getOutcome)))
        (is (str/includes? (.getOutput result) "2 tests completed, 1 failed"))
        (gradle/verify-compilation-without-aot "src" "build/classes/clojure/main")))))
