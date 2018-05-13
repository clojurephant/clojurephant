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
        (gradle/verify-compilation-without-aot "src/main/clojure" "build/classes/clojure/main"))

      (file/delete (gradle/file "src/main/clojure/basic_project/boom.clj"))
      (let [result (gradle/build-and-fail "compileClojure")]
        (is (= TaskOutcome/FAILED (some-> result (.task ":compileClojure") .getOutcome)))))))

(deftest no-clojure
  (testing "without Clojure on classpath, build fails"
    (gradle/with-project "BasicClojureProjectTest"
      (file/write-str (gradle/file "build.gradle") (str/replace (file/read-str (gradle/file "build.gradle"))
                                                                #"compile 'org.clojure:clojure:1.8.0'"
                                                                ""))
      (let [result (gradle/build-and-fail "clean" "check")]
        (is (= TaskOutcome/FAILED (some-> result (.task ":compileClojure") .getOutcome)))
        (is (str/includes? (.getOutput result) "Could not find or load main class clojure.main"))))))
