(ns module-b.core
  (:require [module-a.core :as module-a]))

(defn welcome [name]
  (module-a/hello name))
