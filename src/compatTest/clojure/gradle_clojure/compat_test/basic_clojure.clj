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
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":testClojure") .getOutcome)))
        (gradle/verify-compilation-without-aot "src/main/clojure" "build/classes/clojure/main"))))

  (testing "with AOT compile, only class files are copied to the output directory"
    (gradle/with-project "BasicClojureProjectTest"
      (let [result (gradle/build "-DaotCompile=true" "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileTestClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":testClojure") .getOutcome)))
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

(deftest clojure-test-fail
  (testing "clojure.test failures cause the build to fail"
    (gradle/with-project "TestFailureFailsBuildTest"
      (let [result (gradle/build-and-fail "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileTestClojure") .getOutcome)))
        (is (= TaskOutcome/FAILED (some-> result (.task ":testClojure") .getOutcome)))))))

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
