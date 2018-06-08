(ns gradle-clojure.tools.clojure-loader
  (:require [gradle-clojure.tools.logger :refer [log]]
            [clojure.edn :as edn]
            [clojure.string :as string])
  (:import [gradle_clojure.tools.internal LineProcessingWriter]
           [java.io File]))

(def reflection-warnings (atom {:total 0 :project 0 :library 0}))

(defn processor [source-dirs]
  (fn [line]
    (when-let [warning (re-find #"Reflection warning, (.+?):.*" line)]
      (let [source-file (get warning 1)]
        (swap! reflection-warnings update :total inc)
        (if (some #(.exists (File. % source-file)) source-dirs)
          (swap! reflection-warnings update :project inc)
          (swap! reflection-warnings update :library inc))))))

(defn -main [& args]
  (log :debug "Classpath: %s" (System/getProperty "java.class.path"))
  (let [[source-dirs namespaces reflection] (edn/read)]
    (try
      (binding [*err* (LineProcessingWriter. *err* (processor source-dirs))
                *warn-on-reflection* (#{:warn :fail} reflection)]
        (doseq [namespace namespaces]
          (let [ns-file (-> namespace
                            (string/replace \- \_)
                            (string/replace \. \/))]
            (log :info "Compiling %s" namespace)
            (load ns-file))))

      (if (and (= :fail reflection) (< 0 (:project @reflection-warnings)))
        (throw (ex-info (str "Reflection warnings found: " @reflection) {})))

      (catch Throwable e
        (loop [ex e]
          (if-let [msg (and ex (.getMessage ex))]
            (log :error msg)
            (recur (.getCause ex))))
        (throw e)))))
