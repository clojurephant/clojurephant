(ns gradle-clojure.tools.clojurescript-compiler
  (:require [gradle-clojure.tools.logger :refer [log]]
            [cljs.build.api :as api]
            [clojure.edn :as edn]
            [clojure.pprint]
            [clojure.java.io :as io]))

(defn compile-cljs
  [source-dirs compiler-options]
  (let [sources (filter #(.exists (io/file %)) source-dirs)]
    (try
      (api/build
       (apply api/inputs sources)
       compiler-options)
      (catch Throwable e
        (loop [ex e]
          (if-let [msg (and ex (.getMessage ex))]
            (log :error msg)
            (recur (.getCause ex))))
        (throw e)))))

(defn -main []
  (let [[source-dirs compiler-options] (edn/read)]
    (compile-cljs source-dirs compiler-options)))
