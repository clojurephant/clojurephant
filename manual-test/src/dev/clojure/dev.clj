(ns dev
  (:require [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
            [sample.core :as core]
            [cider.piggieback :as piggie]
            [figwheel.repl :as frepl]))

(set-init core/make-system)

(defonce env
  (frepl/repl-env
   :port 5050
   :open-url "http://localhost:8080/index.html"
   :output-dir "build/tmp/clojureRepl/public/js/out"))

(defn cljs-repl []
  (piggie/cljs-repl env))
