(ns sample.dev
  (:require [figwheel.repl :as repl]))

(repl/connect "ws://localhost:5050/figwheel-connect")

(js/console.log "I preloaded so fast!")
