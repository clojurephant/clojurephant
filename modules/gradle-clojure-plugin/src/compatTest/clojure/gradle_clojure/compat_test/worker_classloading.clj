(ns gradle-clojure.compat-test.worker-classloading
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest leaking-gradle
  (testing "ClojureWorker does not leak classes (such as Guava)"
    (gradle/with-project "WorkerClassLoadingTest"
      (let [result (gradle/build "leakingGuava")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":leakingGuava") .getOutcome)))
        (is (str/includes? (.getOutput result) "Guava was not found on the classpath"))))))

(deftest leaking-old-clojure
  (testing "ClojureWorker participates in conflict resolution"
    (gradle/with-project "WorkerClassLoadingTest"
      (let [result (gradle/build "leakingClojure")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":leakingClojure") .getOutcome)))))))
