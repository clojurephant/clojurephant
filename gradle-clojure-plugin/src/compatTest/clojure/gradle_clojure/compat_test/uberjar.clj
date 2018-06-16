(ns gradle-clojure.compat-test.uberjar
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]
           [java.time LocalDate]))

(deftest uberjar-application
  (testing "an application uberjar can have its main ns run"
    (gradle/with-project "UberjarTest"
      (let [result (gradle/build "runShadow" "-q")]
        (gradle/verify-task-outcome result ":runShadow" :success)
        (is (str/includes? (.getOutput result) (str (LocalDate/now))))))))
