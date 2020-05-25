(ns clja.core
  (:require [cljb.core :as cljb]))

(defn process []
  (:my-key (cljb/read-data)))
