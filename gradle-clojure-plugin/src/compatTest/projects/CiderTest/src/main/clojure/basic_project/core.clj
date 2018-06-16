(ns basic-project.core
  (:require [clojure.java.io :as io]
            [basic-project.boom :as boom]))

(defprotocol ITest)

(defn hello [name]
  (println "Generating message for" name)
  (str "Hello " name))

(defn bad [name]
  (.endsWith name " Smith"))

(defn use-ns [x]
  (println "This is answer: " (boom/stuff x)))

(println (slurp (io/resource "file.txt")))
