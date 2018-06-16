(ns gradle-clojure.compat-test.repl
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file]
            [nrepl.core :as repl])
  (:import [org.gradle.testkit.runner TaskOutcome]
           [gradle_clojure.compat_test LineProcessingWriter]
           [java.time LocalDate]))

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

(defn send-repl [client msg]
  (let [response (first (repl/message client msg))]
    (or (:value response) response)))

(defn eval-repl [client form]
  (send-repl client {:op "eval" :code (pr-str form)}))

(defmacro with-client [[client project & args] & body]
  `(let [port-promise# (promise)
         build-thread# (Thread. #(start-repl port-promise# ~project ~@args))]
     (.start build-thread#)
     (let [port# (deref port-promise# 30000 :timeout)]
       (if (int? port#)
         (with-open [conn# (repl/connect :port port#)]
           (let [~client (repl/client conn# 1000)]
             (try
               ~@body
               (finally
                 (repl/message ~client {:op "eval" :code (pr-str '(do (require 'gradle-clojure.tools.clojure-nrepl)  (gradle-clojure.tools.clojure-nrepl/stop!)))})))))
         (throw (ex-info "Could not determine port REPL started on." {}))))))

(deftest mixed-java-clojure
  (testing "Java classes are included on classpath of the REPL"
    (with-client [client "MixedJavaClojureTest"]
      (is (= "\"Example2\"" (eval-repl client '(do (require 'cljSS.core) (cljSS.core/test-all))))))))

(deftest custom-handler
  (testing "A custom nREPL handler function can be provided"
    (with-client [client "MixedJavaClojureTest" "--handler=dev/silly-handler"]
      (is (= "\"Keep trying!\"" (eval-repl client "nonsense"))))))

(deftest custom-middleware
  (testing "Custom nREPL middlewares can be provided"
    (with-client [client "MixedJavaClojureTest" "--middleware=dev/current-date" "--middleware=dev/number-1"]
      (is (= (#{(str (LocalDate/now)) (pr-str (str (LocalDate/now)))} (send-repl client {:op "now"}))))
      ;; I have no idea why locally (Win10) I get "\"one\"" back, but in Circle CI, I get "one"
      (is (#{"one" (pr-str "one")} (send-repl client {:op "num1"}))))))

(deftest cider
  (testing "CIDER middlewares can be provided"
    (with-client [client "CiderTest" "--handler=cider.nrepl/cider-nrepl-handler"]
      (is (= "0.17.0" (-> (send-repl client {:op "cider-version"}) :cider-version :version-string)))
      (is (pr-str 7) (eval-repl client '(do (require 'basic-project.core) (basic-project/use-ns 4)))))))

(deftest task-dependencies
  (testing "No Clojure compiles happen when REPL is requested, but other languages are compiled"
    (gradle/with-project "MixedJavaClojureTest"
      (file/write-str (gradle/file "build.gradle") "clojureRepl { doFirst { throw new GradleException(\"Fail!\") } }\n" :append true)
      (let [result (gradle/build-and-fail "clojureRepl")]
        (gradle/verify-task-outcome result ":clojureRepl" :failed)
        (gradle/verify-task-outcome result ":compileJava" :success :no-source)
        (gradle/verify-task-outcome result ":compileTestJava" :success :no-source)
        (gradle/verify-task-outcome result ":compileDevJava" :success :no-source)
        (gradle/verify-task-outcome result ":checkDevClojure" :success)
        (gradle/verify-task-outcome result ":checkClojure" :skipped)
        (gradle/verify-task-outcome result ":checkTestClojure" :skipped)
        (gradle/verify-task-outcome result ":compileClojure" :skipped)
        (gradle/verify-task-outcome result ":compileTestClojure" :skipped)
        (gradle/verify-task-outcome result ":compileDevClojure" :skipped)))))
        ; (gradle/verify-task-outcome result ":compileClojureScript" :skipped)
        ; (gradle/verify-task-outcome result ":compileTestClojureScript" :skipped)
        ; (gradle/verify-task-outcome result ":compileDevClojureScript" :skipped)))))
