(ns gradle-clojure.compat-test.basic-clojurescript
  (:require [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest basic-build
  (testing "simple build"
    (gradle/with-project "BasicClojureScriptProjectTest"
      (let [result (gradle/build "classesAot")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":compileClojurescript") .getOutcome)))
        (is (seq (gradle/file-tree "build/classes/clojurescript/main/js/out")))
        (is (true? (file/exists? (gradle/file "build/classes/clojurescript/main/js/main.js"))))
        (is (true? (file/exists? (gradle/file "build/classes/clojurescript/main/js/main.js.map"))))))))
