(ns gradle-clojure.tools.figwheel
  (:require [figwheel.main :as fw]
            [gradle-clojure.tools.clojure-nrepl :as repl]
            [clojure.java.io :as io])
  (:import [java.nio.file Files Paths LinkOption]))

(defn re-root [path from to]
  (str (.resolve to (.relativize from (Paths/get path (into-array String []))))))

(defn map-vals [f m]
  (reduce (fn [m2 [k v]] (assoc m2 k (f v))) {} m))

(defn re-root-modules [build from-dir to-dir]
  (if (get-in build [:compiler :modules])
    (update-in build [:compiler :modules] (fn [modules]
                                            (map-vals #(update-in % [:output-to] re-root from-dir to-dir) modules)))
    build))

(defn re-root-source-map [build from-dir to-dir]
  (if (string? (get-in build [:compiler :source-map]))
    (update-in build [:compiler :source-map] re-root from-dir to-dir)
    build))

(defn re-root-build [build output-dir]
  (let [from-dir (Paths/get (:output-dir build) (into-array String []))
        to-dir (Paths/get output-dir (into-array String []))]
    (-> build
        (update-in [:compiler :output-to] re-root from-dir to-dir)
        (update-in [:compiler :output-dir] re-root from-dir to-dir)
        (update-in [:figwheel :target-dir] re-root from-dir to-dir)
        (re-root-source-map from-dir to-dir)
        (re-root-modules from-dir to-dir))))

(defn real-dirs [dirs]
  (let [xf (comp (map #(Paths/get % (into-array String [])))
                 (filter #(Files/exists % (into-array LinkOption [])))
                 (map str))]
    (into [] xf dirs)))

(defn build-opts [conf id]
  (let [build (-> (get-in conf [:cljs-builds id])
                  (re-root-build (:output-dir conf))
                  (update-in [:figwheel :watch-dirs] real-dirs)
                  (update-in [:figwheel :css-dirs] real-dirs))]
    {:id id
     :options (:compiler build)
     :config (:figwheel build)}))

(defn start [id & background-ids]
  (let [conf @repl/context
        figwheel-opts (:figwheel conf)
        main-build (build-opts conf id)
        background-builds (map #(build-opts conf %) background-ids)]
    (apply fw/start figwheel-opts main-build background-builds)))
