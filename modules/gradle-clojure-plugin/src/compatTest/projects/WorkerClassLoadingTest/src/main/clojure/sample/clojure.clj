(ns sample.clojure
  (:require [clojure.java.classpath :as cp]))

(defn -main [& args]
  (let [clojure (->> (cp/classpath)
                     (map #(.getName %))
                     (filter #(re-matches #"clojure-\d+\.\d+\.\d+\.jar" %))
                     (into []))]
    (if (= 1 (count clojure))
      (println "Only one version of clojure on the classpath")
      (throw (ex-info "Multiple versions of clojure on the classpath" {:clojure clojure})))))
