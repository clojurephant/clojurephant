(ns cljSS.core
  (:import (javaSS Example1 Example2)))

(defn test-all []
  (.test (Example1.))
  (.test (Example2.)))
