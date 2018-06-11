(ns gradle-clojure.tools.figwheel
  (:require [figwheel.main :as fw]))

(defonce config (atom {:figwheel {}
                       :builds {"main" {:compiler {}
                                        :figwheel {}}
                                "dev" {:compiler {}
                                       :figwheel {}}}}))

(defn build-opts [conf id]
  {:id id
   :options (get-in conf [:builds id :compiler])
   :config (get-in conf [:builds id :figwheel])})

(defn start [id & background-ids]
  (let [conf @config
        figwheel-opts (:figwheel conf)
        main-build (build-opts conf id)
        background-builds (map #(build-opts conf %) background-ids)]
    (apply fw/start figwheel-opts main-build background-builds)))
