(ns gradle-clojure.tools.clojure-compiler
  (:import [gradle_clojure.tools.internal LineProcessingWriter]
           [java.io File]))

(def ^:dynamic *namespaces*)

(def reflection (atom {:total 0 :project 0 :library 0}))

(defn processor [source-dirs]
  (fn [line]
    (when-let [warning (re-find #"Reflection warning, (.+?):.*" line)]
      (let [source-file (get warning 1)]
        (swap! reflection update :total inc)
        (if (some #(.exists (File. % source-file)) source-dirs)
          (swap! reflection update :project inc)
          (swap! reflection update :library inc))))))

(defn reflection? [compile-options]
  (let [opts (.getReflectionWarnings compile-options)]
    (cond
      (not (.isAsErrors opts)) false
      (.isProjectOnly opts) (< 0 (:project @reflection))
      :else (< 0 (:total @reflection)))))

(defn compiler [source-dirs destination-dir compile-options namespaces]
  (try
    (binding [*namespaces* (seq namespaces)
              *err* (LineProcessingWriter. *err* (processor source-dirs))
              *compile-path* (.getAbsolutePath destination-dir)
              *warn-on-reflection* (-> compile-options .getReflectionWarnings .isEnabled)
              *compiler-options* {:disable-locals-clearing (.isDisableLocalsClearing compile-options)
                                  :elide-meta (into [] (map keyword) (.getElideMeta compile-options))
                                  :direct-linking (.isDirectLinking compile-options)}]
      (doseq [namespace namespaces]
        (compile (symbol namespace))))
    (if (reflection? compile-options)
      (throw (ex-info (str "ERROR: Reflection warnings found: " @reflection) {})))
    (catch Throwable e
      (binding [*out* *err*]
        (loop [ex e]
          (if-let [msg (and ex (.getMessage ex))]
            (println "ERROR: " (-> ex .getClass .getCanonicalName) " " msg)
            (recur (.getCause ex)))))
      (throw e))))
