(ns gradle-clojure.compat-test.basic-clojure
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest basic-build
  (testing "with AOT compile, only class files are copied to the output directory"
    (gradle/with-project "BasicClojureProjectTest"
      (let [result (gradle/build "classesAot")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (gradle/verify-compilation-with-aot "src/main/clojure" "build/classes/clojure/main")))))

(deftest stale-outputs
  (testing "with AOT compile, stale output files are cleaned"
    (gradle/with-project "BasicClojureProjectTest"
      (let [result (gradle/build "classesAot")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (gradle/verify-compilation-with-aot "src/main/clojure" "build/classes/clojure/main"))

      (file/delete (gradle/file "src/main/clojure/basic_project/utils.clj"))
      (let [result (gradle/build "classesAot")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (gradle/verify-compilation-with-aot "src/main/clojure" "build/classes/clojure/main"))

      (file/delete (gradle/file "src/main/clojure/basic_project/boom.clj"))
      (let [result (gradle/build-and-fail "classesAot")]
        (is (= TaskOutcome/FAILED (some-> result (.task ":compileClojure") .getOutcome)))))))

(deftest no-clojure
  (testing "without Clojure on classpath, build fails"
    (gradle/with-project "BasicClojureProjectTest"
      (file/write-str (gradle/file "build.gradle") (str/replace (file/read-str (gradle/file "build.gradle"))
                                                                #"implementation 'org.clojure:clojure:1.8.0'"
                                                                ""))
      (let [result (gradle/build-and-fail "clean" "check")]
        (is (= TaskOutcome/FAILED (some-> result (.task ":checkClojure") .getOutcome)))
        (is (str/includes? (.getOutput result) "Could not find or load main class clojure.main"))))))

(deftest jar
  (testing "only source files are included in jar"
    (gradle/with-project "BasicClojureProjectTest"
      (let [result (gradle/build "jar")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":jar") .getOutcome)))
        (gradle/verify-jar-contents ["src/main/clojure" "src/main/resources"] "build/libs/BasicClojureProjectTest.jar")))))

(deftest aot-jar
  (testing "only class files are included in jar"
    (gradle/with-project "BasicClojureProjectTest"
      (let [result (gradle/build "aotJar")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":aotJar") .getOutcome)))
        (gradle/verify-jar-contents ["build/classes/clojure/main" "src/main/resources"] "build/libs/BasicClojureProjectTest.jar")))))
