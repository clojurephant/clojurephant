(ns dev
  (:require [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
            [sample.core :as core]
            [dev.clojurephant.tooling.figwheel-main :as fig]))

(set-init core/make-system)
