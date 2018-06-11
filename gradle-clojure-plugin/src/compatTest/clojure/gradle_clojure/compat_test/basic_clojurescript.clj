(ns gradle-clojure.compat-test.basic-clojurescript
  (:require [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest basic-build
  (testing "simple build"
    (gradle/with-project "BasicClojureScriptProjectTest"
      (let [result (gradle/build "classes")]
        (gradle/verify-task-outcome result ":compileClojureScript" :success)
        (is (seq (gradle/file-tree "build/clojurescript/main/js/out")))
        (is (true? (file/exists? (gradle/file "build/clojurescript/main/js/main.js"))))
        (is (true? (file/exists? (gradle/file "build/clojurescript/main/js/main.js.map"))))))))
