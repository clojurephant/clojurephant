(ns gradle-clojure.compat-test.repl
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file]
            [clojure.tools.nrepl :as repl])
  (:import [org.gradle.testkit.runner TaskOutcome]
           [gradle_clojure.compat_test LineProcessingWriter]))

(defn parse-port [port]
  (fn [line]
    (if-let [match (re-find #"nREPL server started on port\s+(\d+)" line)]
      (deliver port (Integer/parseInt (nth match 1))))))

(defn start-repl [port project & args]
  (gradle/with-project project
    (let [runner (gradle/runner (concat ["clojureRepl"] args))
          writer (LineProcessingWriter. *out* (parse-port port))]
      (.forwardStdOutput runner writer)
      (.forwardStdError runner writer)
      (try
        (.build runner)
        (finally
          (deliver port :build-failed))))))

(defn eval-repl [client form]
  (let [response (first (repl/message client {:op "eval" :code (pr-str form)}))]
    (or (:value response) response)))

(deftest mixed-java-clojure
  (testing "Java classes are included on classpath of the REPL"
    (let [port-promise (promise)
          build-thread (Thread. #(start-repl port-promise "MixedJavaClojureTest"))]
      (.start build-thread)
      (let [port (deref port-promise 30000 :timeout)]
        (if (int? port)
          (with-open [conn (repl/connect :port port)]
            (let [client (repl/client conn 1000)]
              (is (= "\"Example2\"" (eval-repl client '(do (require 'cljSS.core) (cljSS.core/test-all)))))
              (repl/message client {:op "eval" :code (pr-str '(do (require 'gradle-clojure.tools.clojure-nrepl) (gradle-clojure.tools.clojure-nrepl/stop!)))})))
          (throw (ex-info "Could not determine port REPL started on." {})))))))
