(ns sample.core)

(defn hello [name]
  (println "Hello" name))

(defn ends? [string suffix]
  (.endsWith string suffix))
