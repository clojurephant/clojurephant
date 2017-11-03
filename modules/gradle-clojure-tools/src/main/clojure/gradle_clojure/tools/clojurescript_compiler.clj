(ns gradle-clojure.tools.clojurescript-compiler
  (:require [cljs.build.api :as api]
            [clojure.edn :as edn]
            [clojure.pprint]))

(defn compile-cljs
  [source-dirs compiler-options]
  (try
    (api/build
      (apply api/inputs source-dirs)
      compiler-options)
    (catch Throwable e
      (binding [*out* *err*]
        (loop [ex e]
          (if-let [msg (and ex (.getMessage ex))]
            (println "ERROR: " (-> ex .getClass .getCanonicalName) " " msg)
            (recur (.getCause ex)))))
      (throw e))))

(defn -main
  [options-file]
  (let [options (edn/read-string (slurp options-file))
        {:keys [source-dirs compiler-options]} options]
    (compile-cljs source-dirs compiler-options)))
