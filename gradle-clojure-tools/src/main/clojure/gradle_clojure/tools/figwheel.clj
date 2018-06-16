(ns gradle-clojure.tools.figwheel
  (:require [figwheel.main :as fw]
            [gradle-clojure.tools.clojure-nrepl :as repl]))

(defn build-opts [conf id]
  {:id id
   :options (get-in conf [:cljs-builds id :compiler])
   :config (get-in conf [:cljs-builds id :figwheel])})

(defn start [id & background-ids]
  (let [conf @repl/context
        figwheel-opts (:figwheel conf)
        main-build (build-opts conf id)
        background-builds (map #(build-opts conf %) background-ids)]
    (apply fw/start figwheel-opts main-build background-builds)))
