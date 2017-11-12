(ns gradle-clojure.tools.clojure-test-junit4
  (:require [clojure.string :as string]
            [clojure.test :as test]
            [clojure.main :as main])
  (:import [java.lang.annotation Annotation]
           [org.junit.runner Description]
           [org.junit.runner.notification Failure]
           [java.io File]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Discovery
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord Test [description namespace var])
(defrecord Suite [description namespace tests])

(defn- var-test [var]
  (let [namespace (-> var meta :ns)
        suite (-> namespace str)
        test (-> var meta :name name)
        description (Description/createTestDescription suite test (into-array Annotation []))]
    (->Test description namespace var)))

(defn- test? [var]
  (-> var meta :test))

(defn- ns-suite [clazz]
  (let [root-description (Description/createSuiteDescription clazz)
        ns-sym (-> clazz .getCanonicalName main/demunge symbol)]
    (require ns-sym)
    (let [namespace (find-ns ns-sym)
          tests (->> namespace ns-interns vals (filter test?) (map var-test) (into []))]
      (doseq [test tests]
        (.addChild root-description (:description test)))
      (->Suite root-description namespace tests))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Execution
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *notifier* nil)
(def ^:dynamic *description* nil)

(defmulti report :type)

(defmethod report :begin-test-var [m]
  (.fireTestStarted *notifier* *description*))

(defmethod report :end-test-var [m]
  (.fireTestFinished *notifier* *description*))

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
    (.fireTestFailure *notifier* (Failure. *description* (AssertionError. msg)))))

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
    (.fireTestFailure *notifier* (Failure. *description* (AssertionError. msg actual)))))

;; ignore all others
(defmethod report :begin-test-ns [m])
(defmethod report :end-test-ns [m])
(defmethod report :pass [m])
(defmethod report :default [_])

(defn test-var [test]
  (binding [test/*testing-vars* (conj test/*testing-vars* (:var test))]
    (try
      ((-> test :var meta :test))
      (catch Throwable e
        (test/do-report {:type :error :message "Uncaught exception, not in assertion." :expected nil :actual e})))))

(defn run-test [test each-fixture-fn]
  (binding [*description* (:description test)]
    (test/do-report {:type :begin-test-var :var (:var test)})
    (try
      (each-fixture-fn
        (fn []
          (test-var test)))
      (catch Throwable e
        (test/do-report {:type :error :message "Uncaught exception, in each-fixtures." :expected nil :actual e})))
    (test/do-report {:type :end-test-var :var (:var test)})))

(defn run-suite [suite notifier]
  (binding [test/report report
            *notifier* notifier
            *description* (:description suite)]
    (test/do-report {:type :begin-test-ns :ns (:namespace suite)})
    (let [once-fixture-fn (-> suite :namespace meta ::test/once-fixtures test/join-fixtures)
          each-fixture-fn (-> suite :namespace meta ::test/each-fixtures test/join-fixtures)]
      (try
        (once-fixture-fn
          (fn []
            (doseq [test (:tests suite)]
              (run-test test each-fixture-fn))))
        (catch Throwable e
          (test/do-report {:type :error :message "Uncaught exception, in once-fixtures." :expected nil :actual e}))))
    (test/do-report {:type :end-test-ns :ns (:namespace suite)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; JUnit runner.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(gen-class
  :name gradle_clojure.tools.ClojureTestRunner
  :implements [org.junit.runner.manipulation.Filterable]
  :extends org.junit.runner.Runner
  :constructors {[Class] []}
  :init init
  :state suite)

(defn -init [clazz]
  [[] (atom (ns-suite clazz))])

(defn -getDescription [this]
  (-> this .suite deref :description))

(defn -run [this notifier]
  (run-suite (-> this .suite deref) notifier))

(defn -filter [this desc-filter]
  (letfn [(run? [test] (->> test :description (.shouldRun desc-filter)))
          (trim-tests [tests] (filter run? tests))
          (trim-suite [suite] (update suite :tests trim-tests))]
    (swap! (.suite this) trim-suite)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generate suite stubs.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmacro gen-runners [namespaces]
  (let [nses (eval namespaces)]
    `(do
       ~@(map (fn [ns#]
                (println ns#)
                `(gen-class :name ~(with-meta (symbol ns#) {'org.junit.runner.RunWith 'gradle_clojure.tools.ClojureTestRunner})))
              nses))))
(gen-runners (remove #{"gradle-clojure.tools.clojure-test-junit4"} gradle-clojure.tools.clojure-compiler/*namespaces*))
