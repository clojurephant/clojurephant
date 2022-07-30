(ns sample.core-test
  (:require [sample.core :as core]
            [clojure.test :refer :all]))

(deftest hello-world
  (is (= 4 (+ 2 2))))
