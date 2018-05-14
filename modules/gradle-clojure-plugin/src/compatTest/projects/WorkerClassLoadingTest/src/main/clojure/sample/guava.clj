(ns sample.guava
  (:require [clojure.java.classpath :as cp]))

(defn -main [& args]
  (try
    (if (eval '(com.google.common.base.Objects/equal "1" "2"))
      (throw (ex-info "Incorrectly found objects to be equal" {}))
      (throw (ex-info "Guava should not be on the classpath"
                      {:classpath (map str (cp/classpath))
                       :classloaders (->> (clojure.lang.RT/baseLoader)
                                          (iterate #(.getParent %))
                                          (take-while identity)
                                          (map (fn [loader] (str (.getName loader) ": " (str loader)))))
                       :guava-loader (eval '(-> (com.google.common.base.Functions/identity) (class) (.getClassLoader)))})))
    (catch ClassNotFoundException e
      (println "Guava was not found on the classpath"))))
