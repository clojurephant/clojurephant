(ns sample.core
  (:require [clojure.string :as string]
            [java-time :as time])
  (:gen-class))

(defn -main [& args]
  (println (str (time/local-date))))
