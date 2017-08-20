(ns gradle-clojure.compat-test.java-clojure
  (:require [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file]
            [clojure.string :as str]
            [clojure.set :as set])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest one-source-set
  (gradle/with-project "JavaAndClojureInOneSourceSetTest"
    (testing "with Java and Clojure in one source set, Java classes are not removed during incremental build"
      (let [result (gradle/build "clean" "build")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileJava") .getOutcome)))
        (is (empty? (set/intersection (gradle/file-tree "src/main/clojure") (gradle/file-tree "build/classes/clojure/main"))))
        (is (some #(-> % .getFileName str (str/ends-with? ".class")) (gradle/file-tree "build/classes/clojure/main"))))
      (file/delete (gradle/file "src/main/clojure/clj_example/core.clj"))
      (file/write-str (gradle/file "src/main/clojure/clj_example/utils.clj") "(ns clj-example.utils) (defn ping [] \"pong\")" :append true)
      (let [result (gradle/build "build")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":compileJava") .getOutcome)))
        (is (empty? (set/intersection (gradle/file-tree "src/main/clojure") (gradle/file-tree "build/classes/clojure/main"))))
        (is (some #(-> % .getFileName str (str/ends-with? ".class")) (gradle/file-tree "build/classes/clojure/main")))))))

(deftest java-depend-on-clojure
  (gradle/with-project "MixedClojureJavaTest"
    (testing "with Java code that depends on Clojure code in different source sets, compilation succeeds"
      (let [result (gradle/build "clean" "compileJavaSSJava")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileCljSSClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileJavaSSJava") .getOutcome)))
        (is (empty? (set/intersection (gradle/file-tree "src/cljSS/clojure") (gradle/file-tree "build/classes/clojure/cljSS"))))
        (is (some #(-> % .getFileName str (str/ends-with? ".class")) (gradle/file-tree "build/classes/clojure/cljSS")))
        (is (some #(-> % .getFileName str (str/ends-with? ".class")) (gradle/file-tree "build/classes/java/javaSS")))))))

(deftest clojure-depends-on-java
  (gradle/with-project "MixedJavaClojureTest"
    (testing "with Clojure code that depends on Java code in different source sets, compilation succeeds"
      (let [result (gradle/build "clean" "compileCljSSClojure")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileCljSSClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileJavaSSJava") .getOutcome)))
        (is (empty? (set/intersection (gradle/file-tree "src/cljSS/clojure") (gradle/file-tree "build/classes/clojure/cljSS"))))
        (is (some #(-> % .getFileName str (str/ends-with? ".class")) (gradle/file-tree "build/classes/clojure/cljSS")))
        (is (some #(-> % .getFileName str (str/ends-with? ".class")) (gradle/file-tree "build/classes/java/javaSS")))))
    (testing "with Clojure code that depends on Java code, no recompilation if Java doesn't change"
      (let [result (gradle/build "compileCljSSClojure")]
        (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":compileCljSSClojure") .getOutcome)))
        (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":compileJavaSSJava") .getOutcome)))))
    (testing "with Clojure code that depends on Java code, recompiles if Java changes"
      (file/delete (gradle/file "src/javaSS/java/javaSS/Example1.java"))
      (let [result (gradle/build-and-fail "compileCljSSClojure")]
        (is (= TaskOutcome/FAILED (some-> result (.task ":compileCljSSClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileJavaSSJava") .getOutcome)))))))
