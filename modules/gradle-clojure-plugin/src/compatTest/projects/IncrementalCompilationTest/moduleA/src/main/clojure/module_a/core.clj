(ns module-a.core)

(defprotocol ITest)

(defn hello [name]
  (str "Hello " name))
