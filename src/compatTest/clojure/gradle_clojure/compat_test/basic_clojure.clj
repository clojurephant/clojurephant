(ns gradle-clojure.compat-test.basic-clojure
  (:require [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file]
            [clojure.string :as str]
            [clojure.set :as set])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest basic-build
  (gradle/with-project "BasicClojureProjectTest"
    (testing "without AOT compile, only source files are copied to the output directory"
      (let [result (gradle/build "clean" "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileTestClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":testClojure") .getOutcome)))
        (is (= (gradle/file-tree "src/main/clojure") (gradle/file-tree "build/classes/clojure/main")))))

    (testing "with AOT compile, only class files are copied to the output directory"
      (file/write-str (gradle/file "build.gradle") "compileClojure { options.aotCompile = true }" :append true)
      (let [result (gradle/build "clean" "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileTestClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":testClojure") .getOutcome)))
        (is (empty? (set/intersection (gradle/file-tree "src/main/clojure") (gradle/file-tree "build/classes/clojure/main"))))
        (is (every? #(-> % .getFileName str (str/ends-with? ".class")) (gradle/file-tree "build/classes/clojure/main")))))))

(deftest clojure-test-fail
  (gradle/with-project "TestFailureFailsBuildTest"
    (testing "clojure.test failures cause the build to fail"
      (let [result (gradle/build-and-fail "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileTestClojure") .getOutcome)))
        (is (= TaskOutcome/FAILED (some-> result (.task ":testClojure") .getOutcome)))))))

(deftest incremental-build
  (gradle/with-project "IncrementalCompilationTest"
    (testing "without AOT compile"

      (testing "build is up-to-date when no input changes"
        (let [result (gradle/build "clean" "classes")]
          (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome))))
        (let [result (gradle/build "classes")]
          (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":compileClojure") .getOutcome)))
          (is (= (gradle/file-tree "src/main/clojure") (gradle/file-tree "build/classes/clojure/main")))))

      (testing "build reruns when input changes"
        (let [result (gradle/build "clean" "classes")]
          (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome))))
        (file/write-str (gradle/file "src/main/clojure/basic_project/utils.clj") "(ns basic-project.utils) (defn ping [] \"pong\")" :append true)
        (let [result (gradle/build "classes")]
          (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
          (is (= (gradle/file-tree "src/main/clojure") (gradle/file-tree "build/classes/clojure/main"))))))

    (testing "with AOT compile"
      (file/write-str (gradle/file "build.gradle") "compileClojure { options.aotCompile = true }" :append true)

      (testing "build is up-to-date when no input changes"
        (let [result (gradle/build "clean" "classes")]
          (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome))))
        (let [result (gradle/build "classes")]
          (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":compileClojure") .getOutcome)))
          (is (empty? (set/intersection (gradle/file-tree "src/main/clojure") (gradle/file-tree "build/classes/clojure/main"))))
          (is (every? #(-> % .getFileName str (str/ends-with? ".class")) (gradle/file-tree "build/classes/clojure/main")))))

      (testing "build reruns when input changes"
        (let [result (gradle/build "clean" "classes")]
          (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome))))
        (file/write-str (gradle/file "src/main/clojure/basic_project/utils.clj") "(ns basic-project.utils) (defn ping [] \"pong\")" :append true)
        (let [result (gradle/build "classes")]
          (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
          (is (empty? (set/intersection (gradle/file-tree "src/main/clojure") (gradle/file-tree "build/classes/clojure/main"))))
          (is (every? #(-> % .getFileName str (str/ends-with? ".class")) (gradle/file-tree "build/classes/clojure/main"))))))))

(deftest multiple-source-sets
  (gradle/with-project "MultipleSourceSetsTest"
    (testing "with multiple source sets, each gets its own set of tasks to compile the corresponding code"
      (let [result (gradle/build "clean" "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileSs1Clojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileSs2Clojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileTestClojure") .getOutcome)))
        (is (= (gradle/file-tree "src/ss1/clojure") (gradle/file-tree "build/classes/clojure/ss1")))
        (is (empty? (set/intersection (gradle/file-tree "src/ss2/clojure") (gradle/file-tree "build/classes/clojure/ss2"))))
        (is (every? #(-> % .getFileName str (str/ends-with? ".class")) (gradle/file-tree "build/classes/clojure/ss2")))))))
