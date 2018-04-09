(ns basic-project.core
  (:require [clojure.java.io :as io]))

(defprotocol ITest)

(defn hello [name]
  (println "Generating message for" name)
  (str "Hello " name))

(defn bad [name]
  (.endsWith name " Smith"))

(println (slurp (io/resource "file.txt")))
