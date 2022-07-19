(ns dev.clojurephant.compat-test.uberjar
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [dev.clojurephant.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]
           [java.time LocalDate]))

(deftest uberjar-application
  (testing "an application uberjar can have its main ns run"
    (gradle/with-project "UberjarTest"
      ;; Shadow plugin 7.1.2 is incompatible with configuration cache https://github.com/johnrengelman/shadow/issues/775
      (let [result (gradle/build "runShadow" "-q" "--no-configuration-cache")]
        (gradle/verify-task-outcome result ":runShadow" :success)
        (is (str/includes? (.getOutput result) (str (LocalDate/now))))))))
