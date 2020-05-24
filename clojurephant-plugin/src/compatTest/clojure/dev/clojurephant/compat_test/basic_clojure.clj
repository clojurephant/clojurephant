(ns dev.clojurephant.compat-test.basic-clojure
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [dev.clojurephant.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest basic-build
  (testing "with AOT compile, only class files are copied to the output directory"
    (gradle/with-project "BasicClojureProjectTest"
      (file/write-str (gradle/file "build.gradle") "clojure { builds { main { aotAll() } } }\n" :append true)
      (let [result (gradle/build "classes")]
        (gradle/verify-task-outcome result ":compileClojure" :success)
        (gradle/verify-task-outcome result ":checkClojure" :success)
        (gradle/verify-compilation-with-aot "src/main/clojure" "build/clojure/main")))))

(deftest stale-outputs
  (testing "with AOT compile, stale output files are cleaned"
    (gradle/with-project "BasicClojureProjectTest"
      (file/write-str (gradle/file "build.gradle") "clojure { builds { main { aotAll() } } }\n" :append true)
      (let [result (gradle/build "classes")]
        (gradle/verify-task-outcome result ":compileClojure" :success)
        (gradle/verify-task-outcome result ":checkClojure" :success)
        (gradle/verify-compilation-with-aot "src/main/clojure" "build/clojure/main"))

      (file/delete (gradle/file "src/main/clojure/basic_project/utils.clj"))
      (let [result (gradle/build "classes")]
        (gradle/verify-task-outcome result ":compileClojure" :success)
        (gradle/verify-task-outcome result ":checkClojure" :success)
        (gradle/verify-compilation-with-aot "src/main/clojure" "build/clojure/main"))

      (file/delete (gradle/file "src/main/clojure/basic_project/boom.clj"))
      (let [result (gradle/build-and-fail "classes")]
        (gradle/verify-task-outcome result ":checkClojure" :failed)))))

(deftest no-clojure
  (testing "without Clojure on classpath, build fails"
    (gradle/with-project "BasicClojureProjectTest"
      (file/write-str (gradle/file "build.gradle") (str/replace (file/read-str (gradle/file "build.gradle"))
                                                                #"implementation 'org.clojure:clojure:1.8.0'"
                                                                ""))
      (let [result (gradle/build-and-fail "clean" "check")]
        (gradle/verify-task-outcome result ":checkClojure" :failed)
        (is (str/includes? (.getOutput result) "Could not find or load main class clojure.main"))))))

(deftest jar
  (testing "only source files are included in jar"
    (gradle/with-project "BasicClojureProjectTest"
      (let [result (gradle/build "jar")]
        (gradle/verify-task-outcome result ":compileClojure" :skipped)
        (gradle/verify-task-outcome result ":checkClojure" :success)
        (gradle/verify-task-outcome result ":jar" :success)
        (gradle/verify-jar-contents ["src/main/clojure" "src/main/resources"] "build/libs/BasicClojureProjectTest.jar")))))

(deftest aot-jar
  (testing "only class files are included in jar"
    (gradle/with-project "BasicClojureProjectTest"
      (file/write-str (gradle/file "build.gradle") "clojure { builds { main { aotAll() } } }\n" :append true)
      (let [result (gradle/build "aotJar")]
        (gradle/verify-task-outcome result ":compileClojure" :success)
        (gradle/verify-task-outcome result ":checkClojure" :success)
        (gradle/verify-task-outcome result ":aotJar" :success)
        (gradle/verify-jar-contents ["build/clojure/main" "src/main/resources"] "build/libs/BasicClojureProjectTest-aot.jar")))))

(deftest aot-specific-namespaces
  (testing "with AOT compile, can compile specific namespaces"
    (gradle/with-project "BasicClojureProjectTest"
      (file/write-str (gradle/file "build.gradle") "clojure { builds { main { aotNamespaces = ['basic-project.core'] } } }\n" :append true)
      (let [result (gradle/build "classes")]
        (gradle/verify-task-outcome result ":compileClojure" :success)
        (gradle/verify-task-outcome result ":checkClojure" :success)
        (gradle/verify-compilation-with-aot "src/main/clojure" "build/clojure/main")))))
