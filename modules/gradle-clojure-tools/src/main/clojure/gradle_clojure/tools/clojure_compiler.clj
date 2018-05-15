(ns gradle-clojure.tools.clojure-compiler
  (:require [gradle-clojure.tools.logger :refer [log]]
            [clojure.edn :as edn])
  (:import [gradle_clojure.tools.internal LineProcessingWriter]
           [java.io File]))

(def ^:dynamic *namespaces*)

(def reflection (atom {:total 0 :project 0 :library 0}))

(defn processor [source-dirs]
  (fn [line]
    (when-let [warning (re-find #"Reflection warning, (.+?):.*" line)]
      (let [source-file (get warning 1)]
        (swap! reflection update :total inc)
        (if (some #(.exists (File. % source-file)) source-dirs)
          (swap! reflection update :project inc)
          (swap! reflection update :library inc))))))

(defn reflection? [options]
  (cond
    (not (-> options :reflection-warnings :as-errors)) false
    (-> options :reflection-warnings :project-only) (< 0 (:project @reflection))
    :else (< 0 (:total @reflection))))

(defn -main [& args]
  (let [[source-dirs destination-dir namespaces options] (edn/read)]
    (try
      (binding [*namespaces* (seq namespaces)
                *err* (LineProcessingWriter. *err* (processor source-dirs))
                *compile-path* destination-dir
                *warn-on-reflection* (-> options :reflection-warnings :enabled)
                *compiler-options* (-> options :compiler-options)]
        (doseq [namespace namespaces]
          (log :debug "Compiling %s" namespace)
          (compile (symbol namespace))))
      (if (reflection? options)
        (throw (ex-info (str "Reflection warnings found: " @reflection) {})))
      (catch Throwable e
        (loop [ex e]
          (if-let [msg (and ex (.getMessage ex))]
            (log :error msg)
            (recur (.getCause ex))))
        (throw e)))))
