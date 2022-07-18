(ns ss2.core
    (:require [ss1.core]))

(defn hello []
  (str "SourceSet2 " (ss1.core/hello)))
