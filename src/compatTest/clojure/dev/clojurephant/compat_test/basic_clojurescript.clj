(ns dev.clojurephant.compat-test.basic-clojurescript
  (:require [clojure.test :refer :all]
            [dev.clojurephant.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest basic-build
  (testing "simple build"
    (gradle/with-project "BasicClojureScriptProjectTest"
      (let [result (gradle/build "classes")]
        (gradle/verify-task-outcome result ":compileClojureScript" :success)
        (is (seq (gradle/file-tree "build/clojurescript/main/public/js/out")))
        (is (true? (file/exists? (gradle/file "build/clojurescript/main/public/js/main.js"))))
        (is (true? (file/exists? (gradle/file "build/clojurescript/main/public/js/main.js.map"))))))))

(deftest dev-build
  (testing "dev build"
    (gradle/with-project "BasicClojureScriptProjectTest"
      (let [result (gradle/build "compileDevClojureScript")]
        (gradle/verify-task-outcome result ":compileDevClojureScript" :success)
        (is (seq (gradle/file-tree "build/clojurescript/dev/public/js/out")))
        (is (true? (file/exists? (gradle/file "build/clojurescript/dev/public/js/main.js"))))))))
