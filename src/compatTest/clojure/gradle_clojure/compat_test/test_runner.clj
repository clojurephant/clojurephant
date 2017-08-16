(ns gradle-clojure.compat-test.test-runner
  (:require [clojure.test :as test]
            [clojure.tools.namespace.find :refer [find-namespaces]])
  (:import (java.lang.annotation Annotation)
           (org.junit.runner Description)
           (org.junit.runner.notification Failure)
           (java.io File)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Discovery
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord Test [description namespace var])
(defrecord Suite [namespace tests])

(defn- var-test [var]
  (let [ns (-> var meta :ns)
        ns-name (str ns)
        var-name (-> var meta :name name)
        description (Description/createTestDescription ns-name var-name (into-array Annotation []))]
    (->Test description ns var)))

(defn- test? [var]
  (-> var meta :test))

(defn- ns-suite [ns]
  (let [tests (->> ns ns-interns vals (filter test?) (map var-test) (into []))]
    (->Suite ns tests)))

(defn- sym->ns [sym]
  (require sym)
  (find-ns sym))

(defn discover [dirs]
  (->> dirs
       find-namespaces
       (map sym->ns)
       (map ns-suite)
       (filter (comp seq :tests))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Execution
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *throwable* nil)
(def ^:dynamic *entries* nil)

(defmulti report :type)

;; ignore these ones, since we're handling on our own
(defmethod report :begin-test-ns [m])
(defmethod report :end-test-ns [m])
(defmethod report :begin-test-var [m])
(defmethod report :end-test-var [m])

;; success doesn't need to be reported
(defmethod report :pass [m])

(defmethod report :fail [m]
  (let [{:keys [message expected actual]} m
        msg (with-out-str
              (println "FAIL in " (test/testing-vars-str m))
              (when (seq test/*testing-contexts*)
                (println (test/testing-contexts-str)))
              (when message
                (println message))
              (println "expected: " expected)
              (println "  actual: " actual))]
    (reset! *throwable* (AssertionError. msg))))

(defmethod report :error [m]
  (let [{:keys [message expected actual]} m
        msg (with-out-str
              (println "ERROR in " (test/testing-vars-str m))
              (when (seq test/*testing-contexts*)
                (println (test/testing-contexts-str)))
              (when message
                (println message))
              (println "expected: " expected)
              (print   "  actual: " actual))]
    (reset! *throwable* (AssertionError. msg actual))))

(defmethod report :default [_] nil)

(defn run-test [test fixture notifier]
  (binding [*throwable* (atom nil)
            test/report report]
    (.fireTestStarted notifier (:description test))
    (try
      (fixture (fn []
                 (binding [test/*testing-vars* (conj test/*testing-vars* (:var test))]
                   (let [t (-> test :var meta :test)]
                     (t)))))
      (catch Throwable e
        (reset! *throwable* e)))
    (if-let [e @*throwable*]
      (.fireTestFailure notifier (Failure. (:description test) e)))
    (.fireTestFinished notifier (:description test))))

(defn run-suite [suite notifier]
  (let [suite-fixture (-> suite :namespace meta ::test/once-fixtures test/join-fixtures)
        test-fixture (-> suite :namespace meta ::test/each-fixtures test/join-fixtures)]
    (try
      (suite-fixture (fn []
                       (doseq [test (:tests suite)]
                         (run-test test test-fixture notifier))))
      (catch Throwable e
        (.fireTestFailure notifier (Failure. (Description/createTestDescription (-> suite :namespace str) "once-fixtures" (into-array Annotation [])) e))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; JUnit runner.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(gen-class
  :name gradle_clojure.ClojureTestRunner
  :implements [org.junit.runner.manipulation.Filterable]
  :extends org.junit.runner.Runner
  :constructors {[Class] []}
  :init init
  :state suites)

(defn -init [clazz]
  (let [test-dirs (map #(File. ^String %)
                       (.split (System/getProperty "clojure.test.dirs")
                               (File/pathSeparator)))]
    [[] (atom (discover test-dirs))]))

(defn -getDescription [this]
  (let [root-description (Description/createSuiteDescription "clojure.test.runner" (into-array Annotation []))
        tests (->> this .suites deref (mapcat :tests) (map :description))]
    (doseq [test tests]
      (.addChild root-description test))
    root-description))

(defn -run [this notifier]
  (doseq [suite (-> this .suites deref)]
    (run-suite suite notifier)))

(defn -filter [this desc-filter]
  (letfn [(run? [test] (->> test :description (.shouldRun desc-filter)))
          (trim-tests [tests] (filter run? tests))
          (trim-suite [suite] (update suite :tests trim-tests))
          (trim-suites [suites] (->> suites (map trim-suite) (filter (comp seq :tests))))]
    (swap! (.suites this) trim-suites)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generate suite stub.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(gen-class
  :name ^{org.junit.runner.RunWith gradle_clojure.ClojureTestRunner} clojure.test.runner)
