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

(deftest reflection
  (gradle/with-project "BasicClojureProjectTest"
    (file/write-str (gradle/file "build.gradle") "compileClojure { options.aotCompile = true }\n" :append true)
    (testing "without reflection warnings enabled, nothing is output"
      (let [result (gradle/build "clean" "compileClojure")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (not (str/includes? (.getOutput result) "Reflection warning, basic_project/core.clj:27")))))
    (testing "with reflection warnings enabled"
      (file/write-str (gradle/file "build.gradle") "compileClojure { options.reflectionWarnings.enabled = true }\n" :append true)
      (testing "and asErrors false, warnings are only output"
        (let [result (gradle/build "clean" "compileClojure")]
          (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
          (is (str/includes? (.getOutput result) "Reflection warning, basic_project/core.clj:27"))))
      (testing "and asErrors true, warnings cause build to fail"
        (file/write-str (gradle/file "build.gradle") "compileClojure { options.reflectionWarnings.asErrors = true }\n" :append true)
        (let [result (gradle/build-and-fail "clean" "compileClojure")]
          (is (= TaskOutcome/FAILED (some-> result (.task ":compileClojure") .getOutcome)))
          (is (str/includes? (.getOutput result) "Reflection warning, basic_project/core.clj:27")))))))

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

(deftest incremental-build
  (doseq [aot-enabled? [false]]
    (let [aot-compile-opt (format "-DaotCompile=%b" aot-enabled?)
          verify-compilation (if aot-enabled?
                               gradle/verify-compilation-with-aot
                               gradle/verify-compilation-without-aot)]

      (testing (format "aotCompile = %b" aot-enabled?)

        (testing "build is up-to-date when no input changes"
          (gradle/with-project "IncrementalCompilationTest"
            (let [result (gradle/build aot-compile-opt "clean" "classes")]
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
              (verify-compilation "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
              (verify-compilation "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))
            (let [result (gradle/build aot-compile-opt "classes")]
              (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
              (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
              (verify-compilation "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
              (verify-compilation "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))))

        (testing "change in a leaf module"
          (gradle/with-project "IncrementalCompilationTest"
            (let [result (gradle/build aot-compile-opt "clean" "classes")]
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
              (verify-compilation "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
              (verify-compilation "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))

            (file/write-str (gradle/file "moduleB/src/main/clojure/module_b/utils.clj") "(ns module-b.utils) (defn ping [] \"pong\")" :append true)
            (let [result (gradle/build aot-compile-opt "classes")]
              (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
              (verify-compilation "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
              (verify-compilation "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))))

        (testing "change in module used in another module"
          (gradle/with-project "IncrementalCompilationTest"
            (let [result (gradle/build aot-compile-opt "clean" "classes")]
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
              (verify-compilation "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
              (verify-compilation "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))

            (file/write-str (gradle/file "moduleA/src/main/clojure/module_a/utils.clj") "(ns module-a.utils) (defn ping [] \"pong\")" :append true)
            (let [result (gradle/build aot-compile-opt "classes")]
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
              (verify-compilation "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
              (verify-compilation "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))))))))

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
