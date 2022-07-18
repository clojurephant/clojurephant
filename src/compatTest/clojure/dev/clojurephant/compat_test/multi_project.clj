(ns dev.clojurephant.compat-test.multi-project
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [dev.clojurephant.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest multi-project-classpath
  (testing "with multi-project and AOT off, project B sources and dependencies are on classpath of A"
    (gradle/with-project "MultiProjectTest"
      (let [result (gradle/build "jar")]
        (gradle/verify-task-outcome result ":SubClojureB:compileClojure" :skipped)
        (gradle/verify-task-outcome result ":SubClojureB:checkClojure" :success)
        (gradle/verify-task-outcome result ":SubClojureB:jar" :success)
        (gradle/verify-task-outcome result ":SubClojureA:compileClojure" :skipped)
        (gradle/verify-task-outcome result ":SubClojureA:checkClojure" :success)
        (gradle/verify-task-outcome result ":SubClojureA:jar" :success)
        (gradle/verify-jar-contents ["SubClojureA/src/main/clojure" "SubClojureA/src/main/resources"] "SubClojureA/build/libs/SubClojureA.jar")
        (gradle/verify-jar-contents ["SubClojureB/src/main/clojure" "SubClojureB/src/main/resources"] "SubClojureB/build/libs/SubClojureB.jar")))))
