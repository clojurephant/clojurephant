(ns dev.clojurephant.compat-test.ide
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.xml :as xml]
            [clojure.test :refer :all]
            [dev.clojurephant.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(defn eclipse-file [& paths]
  (-> (apply gradle/file paths)
      (io/reader)
      (xml/parse)
      (xml-seq)))

(defn verify-eclipse-unique-sources [classpath]
  (let [sources (->> classpath
                     (filter (comp #{"src"} :kind :attrs))
                     (map (comp :path :attrs)))]
    (is (= sources (distinct sources)))))

(deftest eclipse-clj
  (testing "eclipse project files are generated correctly"
    (gradle/with-project "BasicClojureProjectTest"
      (let [result (gradle/build "eclipse" "--no-configuration-cache")]
        (gradle/verify-task-outcome result ":eclipse" :success)
        (let [classpath (eclipse-file ".classpath")]
          (verify-eclipse-unique-sources classpath))))))

(deftest eclipse-cljs
  (testing "eclipse project files are generated correctly"
    (gradle/with-project "BasicClojureScriptProjectTest"
      (let [result (gradle/build "eclipse" "--no-configuration-cache")]
        (gradle/verify-task-outcome result ":eclipse" :success)
        (let [classpath (eclipse-file ".classpath")]
          (verify-eclipse-unique-sources classpath))))))
