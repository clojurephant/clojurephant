(ns dev.clojurephant.compat-test.test-kit
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [ike.cljj.file :as file]
            [ike.cljj.stream])
  (:import [org.gradle.testkit.runner BuildResult BuildTask GradleRunner TaskOutcome]
           [java.util.jar JarFile]
           [java.nio.file Path]))

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
  (reduce (fn [parent child] (.resolve parent (file/as-path child))) *project-dir* paths))

(defn file-tree [& paths]
  (let [root (apply file paths)
        xf (comp (drop 1)
                 (filter file/file?)
                 (map (fn [f] (.relativize root f))))]
    (if (file/exists? root)
      (into #{} xf (file/walk root))
      #{})))

(defn verify-compilation-without-aot
  [src-dir dst-dir]
  (is (= (file-tree src-dir) (file-tree dst-dir))))

(defn verify-compilation-with-aot
  [src-dir dst-dir]
  (is (empty? (set/intersection (file-tree src-dir) (file-tree dst-dir))))
  (is (every? #(-> % .getFileName str (str/ends-with? ".class")) (file-tree dst-dir))))

(defn jar-contents
  [path]
  (let [jar-path (.resolve *project-dir* (file/as-path path))]
    (with-open [jar-stream (.stream (JarFile. (.toFile jar-path)))]
      (into #{} (comp (filter (complement #(.isDirectory %)))
                      (filter (complement #(= (.getName %) "META-INF/MANIFEST.MF")))
                      (map #(.getName %))
                      (map file/as-path))
            jar-stream))))

(defn verify-jar-contents
  [src-dirs dst-jar]
  (let [jar-entries (jar-contents dst-jar)
        sources (into #{} (mapcat file-tree) src-dirs)]
    (is (= sources jar-entries))))

(defn runner [args]
  (println "***** Args:" args "*****")
  (-> (GradleRunner/create)
      (.withProjectDir (-> *project-dir* .toFile))
      (.withArguments (into-array String (conj args "--stacktrace" "-Pdev.clojurephant.tools.logger.level=debug")))
      (.withPluginClasspath)
      (.forwardOutput)))

(defn build [& args]
  (.build (runner args)))

(defn build-and-fail [& args]
  (.buildAndFail (runner args)))

(def task-outcomes
  {:failed TaskOutcome/FAILED
   :from-cache TaskOutcome/FROM_CACHE
   :no-source TaskOutcome/NO_SOURCE
   :skipped TaskOutcome/SKIPPED
   :success TaskOutcome/SUCCESS
   :up-to-date TaskOutcome/UP_TO_DATE})

(defn verify-task-outcome [result name & outcomes]
  (let [allowed-outcomes (into #{} (map task-outcomes) outcomes)
        actual-outcome (some-> result (.task name) .getOutcome)]
    (is (get allowed-outcomes actual-outcome) (str "Task " name " did not result in one of " outcomes))))
