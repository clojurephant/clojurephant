(ns gradle-clojure.compat-test.java-clojure
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest one-source-set
  (testing "with Java and Clojure in one source set, Java classes are not removed during incremental build"
    (gradle/with-project "JavaAndClojureInOneSourceSetTest"
      (let [result (gradle/build "clean" "build")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileJava") .getOutcome)))
        (gradle/verify-compilation-with-aot "src/main/clojure" "build/classes/clojure/main"))
      (file/delete (gradle/file "src/main/clojure/clj_example/core.clj"))
      (file/write-str (gradle/file "src/main/clojure/clj_example/utils.clj") "(ns clj-example.utils) (defn ping [] \"pong\")" :append true)
      (let [result (gradle/build "build")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":compileJava") .getOutcome)))
        (gradle/verify-compilation-with-aot "src/main/clojure" "build/classes/clojure/main")))))

(deftest java-depend-on-clojure
  (testing "with Java code that depends on Clojure code in different source sets, compilation succeeds"
    (gradle/with-project "MixedClojureJavaTest"
      (let [result (gradle/build "clean" "compileJavaSSJava")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileCljSSClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileJavaSSJava") .getOutcome)))
        (gradle/verify-compilation-with-aot "src/cljSS/clojure" "build/classes/clojure/cljSS")
        (is (every? #(-> % .getFileName str (str/ends-with? ".class")) (gradle/file-tree "build/classes/java/javaSS")))))))

(deftest clojure-depends-on-java
  (testing "with Clojure code that depends on Java code in different source sets, compilation succeeds"
    (gradle/with-project "MixedJavaClojureTest"
      (let [result (gradle/build "compileCljSSClojure")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileCljSSClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileJavaSSJava") .getOutcome)))
        (gradle/verify-compilation-with-aot "src/cljSS/clojure" "build/classes/clojure/cljSS")
        (is (every? #(-> % .getFileName str (str/ends-with? ".class")) (gradle/file-tree "build/classes/java/javaSS")))))))
