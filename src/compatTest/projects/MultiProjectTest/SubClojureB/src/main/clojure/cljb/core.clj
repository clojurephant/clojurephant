(ns cljb.core
  (:require [clojure.data.json :as json]))

(defn read-data []
  (json/read-str "{ \"my-key\": 123 }"))
