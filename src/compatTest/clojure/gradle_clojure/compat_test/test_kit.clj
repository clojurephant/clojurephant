(ns gradle-clojure.compat-test.test-kit
  (:require [ike.cljj.file :as file]
            [clojure.test :refer :all])
  (:import [org.gradle.testkit.runner GradleRunner BuildResult BuildTask TaskOutcome]))

(defn setup-project [name]
  (println "*** " name " ***")
  (let [src-dir (file/path (System/getProperty "stutter.projects") name)
        tmp-dir (.resolve (file/temp-dir (str "stutter-" name)) name)]
    (file/make-dir tmp-dir)
    (file/copy src-dir tmp-dir :recurse true)
    tmp-dir))

(defn cleanup-project [project-dir]
  (file/delete project-dir :recurse true))

(def ^:dynamic *project-dir* nil)

(defmacro with-project [name & body]
  `(binding [*project-dir* (setup-project ~name)]
     ~@body))

(defn file [& paths]
  (reduce (fn [parent child] (.resolve parent child)) *project-dir* paths))

(defn file-tree [& paths]
  (let [root (apply file paths)
        xf (comp (drop 1)
                 (filter file/file?)
                 (map (fn [f] (.relativize root f))))]
    (into #{} xf (file/walk root))))

(defn- runner [args]
  (-> (GradleRunner/create)
      (.withProjectDir (-> *project-dir* .toFile))
      (.withArguments (into-array String args))
      (.withPluginClasspath)
      (.forwardOutput)))

(defn build [& args]
  (.build (runner args)))

(defn build-and-fail [& args]
  (.buildAndFail (runner args)))
