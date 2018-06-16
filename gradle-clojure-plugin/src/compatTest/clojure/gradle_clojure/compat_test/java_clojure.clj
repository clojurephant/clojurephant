(ns gradle-clojure.compat-test.java-clojure
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest java-depend-on-clojure
  (testing "with Java code that depends on Clojure code in different source sets, compilation succeeds"
    (gradle/with-project "MixedClojureJavaTest"
      (let [result (gradle/build "clean" "compileJava")]
        (gradle/verify-task-outcome result ":compilePreClojure" :success)
        (gradle/verify-task-outcome result ":compileJava" :success)
        (gradle/verify-compilation-with-aot "src/pre/clojure" "build/clojure/pre")
        (is (every? #(-> % .getFileName str (str/ends-with? ".class")) (gradle/file-tree "build/classes/java/main")))))))

(deftest clojure-depends-on-java
  (testing "with Clojure code that depends on Java code in same source set, compilation succeeds"
    (gradle/with-project "MixedJavaClojureTest"
      (let [result (gradle/build "compileClojure")]
        (gradle/verify-task-outcome result ":compileClojure" :success)
        (gradle/verify-task-outcome result ":compileJava" :success)
        (gradle/verify-compilation-with-aot "src/main/clojure" "build/clojure/main")
        (is (every? #(-> % .getFileName str (str/ends-with? ".class")) (gradle/file-tree "build/classes/java/main")))))))
