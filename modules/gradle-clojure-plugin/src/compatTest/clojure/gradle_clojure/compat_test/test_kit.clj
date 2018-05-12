(ns gradle-clojure.compat-test.test-kit
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner BuildResult BuildTask GradleRunner TaskOutcome]))

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

(defn verify-compilation-without-aot
  [src-dir dst-dir]
  (is (= (file-tree src-dir) (file-tree dst-dir))))

(defn verify-compilation-with-aot
  [src-dir dst-dir]
  (is (empty? (set/intersection (file-tree src-dir) (file-tree dst-dir))))
  (is (every? #(-> % .getFileName str (str/ends-with? ".class")) (file-tree dst-dir))))

(defn runner [args]
  (-> (GradleRunner/create)
      (.withProjectDir (-> *project-dir* .toFile))
      (.withArguments (into-array String (conj args "--stacktrace")))
      (.withPluginClasspath)
      (.forwardOutput)))

(defn build [& args]
  (.build (runner args)))

(defn build-and-fail [& args]
  (.buildAndFail (runner args)))
