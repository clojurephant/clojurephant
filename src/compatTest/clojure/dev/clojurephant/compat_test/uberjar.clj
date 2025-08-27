(ns dev.clojurephant.compat-test.uberjar
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [dev.clojurephant.compat-test.test-kit :as gradle])
  (:import [java.time LocalDate]))

(deftest uberjar-application
  (testing "an application uberjar can have its main ns run"
    (gradle/with-project (if (str/starts-with? (System/getProperty "compat.gradle.version") "8.")
                           "UberjarLegacyTest"
                           "UberjarTest")
      (let [result (gradle/build "runShadow" "-q")]
        (gradle/verify-task-outcome result ":runShadow" :success)
        (is (str/includes? (.getOutput result) (str (LocalDate/now))))))))
