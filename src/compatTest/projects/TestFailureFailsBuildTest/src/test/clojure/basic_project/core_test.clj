(ns basic-project.core-test
  (:require [basic-project.core :refer [hello]]
            [clojure.test :refer :all]))

(deftest test-hello
  (is (= "Hello World!" (hello "World"))))
