(ns test.core-test
  (:require [ss1.core]
            [ss2.core]
            [clojure.test :refer :all]))

(deftest hello-test
  (println "Test1" (ss1.core/hello))
  (println "Test2" (ss2.core/hello)))
