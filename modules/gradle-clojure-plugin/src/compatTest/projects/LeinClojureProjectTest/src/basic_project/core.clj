(ns basic-project.core)

(defprotocol ITest)

(defn hello [name]
  (println "Generating message for" name)
  (str "Hello " name))

(defn bad [name]
  (.endsWith name " Smith"))
