(ns gradle-clojure.tools.figwheel
  (:require [figwheel.main :as fw]
            [gradle-clojure.tools.clojure-nrepl :as repl]
            [clojure.java.io :as io]))

(defn build-opts [conf id]
  (let [build {:id id
               :options (get-in conf [:cljs-builds id :compiler])
               :config (get-in conf [:cljs-builds id :figwheel])}
        existing-files (fn [dirs]
                         (into [] (comp (map io/file)
                                        (filter #(.exists %))
                                        (map #(.getAbsolutePath %)))
                                  dirs))]
    (-> build
        (update-in [:config :watch-dirs] existing-files)
        (update-in [:config :css-dirs] existing-files))))

(defn start [id & background-ids]
  (let [conf @repl/context
        figwheel-opts (:figwheel conf)
        main-build (build-opts conf id)
        background-builds (map #(build-opts conf %) background-ids)]
    (apply fw/start figwheel-opts main-build background-builds)))
