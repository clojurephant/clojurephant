(ns gradle-clojure.test-kit
  (:require [ike.cljj.file :as file]
            [clojure.test :refer :all])
  (:import [org.gradle.testkit.runner GradleRunner BuildResult BuildTask TaskOutcome]))

(defrecord Project [dir])

(defn setup-project [name]
  (let [src-dir (file/path (System/getProperty "stutter.projects") name)
        tmp-dir (.resolve (file/temp-dir (str "stutter-" name)) name)]
    (file/make-dir tmp-dir)
    (file/copy src-dir tmp-dir :recurse true)
    (->Project tmp-dir)))

(defn cleanup-project [project]
  (file/delete (:dir project) :recurse true))

(defmacro with-project [bindings & body]
  (cond
    (= (count bindings) 0) `(do ~@body)
    (symbol? (bindings 0)) `(let [~(bindings 0) (setup-project ~(bindings 1))]
                              (try
                                (with-open ~(subvec bindings 2) ~@body)
                                (finally
                                  (cleanup-project ~(bindings 0)))))
    :else (throw (IllegalArgumentException. "with-project requires valid let binding forms"))))

(defn- runner [project opts]
  (-> (GradleRunner/create)
      (.withProjectDir (-> project :dir .toFile))
      (.withArguments (into-array String (:args opts)))
      (.withPluginClasspath)))

(defn build [project & {:as opts}]
  (.build (runner project opts)))

(defn build-and-fail [project & {:as opts}]
  (.buildAndFail (runner project opts)))

(deftest sample
  (with-project [project "simple"]
    (let [result (build project :args ["simple" "-q"])]
      (is (= "stuff\r\n" (.getOutput result))))))
