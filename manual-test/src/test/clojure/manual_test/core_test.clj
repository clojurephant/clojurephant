(ns manual-test.core-test
  (:require [manual-test.core :refer :all]
            [clojure.test :refer :all]))

(deftest it-works
  (is (= 1 (+ 1 2))))
