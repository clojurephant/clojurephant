(ns basic-project.core
  (:require-macros
    [basic-project.core :refer [test-macro]])
  (:require
    [basic-project.utils :as utils]))

(enable-console-print!)

(defprotocol ITest)

(defn hello [name]
  (println "Generating message for" name)
  (str "Hello " name))

(println (hello "World"))

(utils/plus 1 2)

(test-macro)
