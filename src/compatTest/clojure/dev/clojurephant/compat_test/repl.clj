(ns dev.clojurephant.compat-test.repl
  (:require [clojure.edn :as edn]
            [clojure.test :refer :all]
            [dev.clojurephant.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file]
            [nrepl.core :as repl])
  (:import [dev.clojurephant.compat_test LineProcessingWriter]
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
     (let [port# (deref port-promise# 60000 :timeout)]
       (if (int? port#)
         (with-open [conn# (repl/connect :port port#)]
           (let [~client (repl/client conn# 1000)]
             (try
               ~@body
               (finally
                 (repl/message ~client {:op "eval" :code (pr-str '(System/exit 0))})))))
         (throw (ex-info "Could not determine port REPL started on." {:port port#}))))
     (.interrupt build-thread#)))

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
      (is (#{(str (LocalDate/now)) (pr-str (str (LocalDate/now)))} (send-repl client {:op "now"})))
      ;; I have no idea why locally (Win10) I get "\"one\"" back, but in Circle CI, I get "one"
      (is (#{"one" (pr-str "one")} (send-repl client {:op "num1"}))))))

(deftest cider
  (testing "CIDER middlewares can be provided"
    (with-client [client "CiderTest" "--handler=cider.nrepl/cider-nrepl-handler"]
      (is (= "0.28.2" (-> (send-repl client {:op "cider-version"}) :cider-version :version-string)))
      (is (pr-str 7) (eval-repl client '(do (require 'basic-project.core) (basic-project/use-ns 4)))))))

(deftest task-dependencies-clj
  (testing "No Clojure compiles happen when REPL is requested, but other languages are compiled"
    (gradle/with-project "MixedJavaClojureTest"
      (file/write-str (gradle/file "build.gradle") "clojureRepl { doFirst { throw new GradleException(\"Fail!\") } }\n" :append true)
      (let [result (gradle/build-and-fail "clojureRepl")]
        (gradle/verify-task-outcome result ":clojureRepl" :failed)
        (gradle/verify-task-outcome result ":compileJava" :success :no-source)
        (gradle/verify-task-outcome result ":compileTestJava" nil)
        (gradle/verify-task-outcome result ":compileDevJava" nil)
        (gradle/verify-task-outcome result ":checkDevClojure" nil)
        (gradle/verify-task-outcome result ":checkClojure" nil)
        (gradle/verify-task-outcome result ":checkTestClojure" nil)
        (gradle/verify-task-outcome result ":compileClojure" nil)
        (gradle/verify-task-outcome result ":compileTestClojure" nil)
        (gradle/verify-task-outcome result ":compileDevClojure" nil)))))

(deftest task-dependencies-cljs
  (testing "No ClojureScript compiles happen when REPL is requested, but other languages are compiled"
    (gradle/with-project "BasicClojureScriptProjectTest"
      (file/write-str (gradle/file "build.gradle") "clojureRepl { doFirst { throw new GradleException(\"Fail!\") } }\n" :append true)
      (let [result (gradle/build-and-fail "clojureRepl")]
        (gradle/verify-task-outcome result ":clojureRepl" :failed)
        (gradle/verify-task-outcome result ":compileJava" :success :no-source)
        (gradle/verify-task-outcome result ":compileTestJava" nil)
        (gradle/verify-task-outcome result ":compileDevJava" nil)
        (gradle/verify-task-outcome result ":compileClojureScript" nil)
        (gradle/verify-task-outcome result ":compileTestClojureScript" nil)
        (gradle/verify-task-outcome result ":compileDevClojureScript" nil)))))


(deftest no-compile-output-on-classpath
  (testing "Compile output from other tasks (besides dev cljs) should not be on classpath of the REPL"
    (with-client [client "BasicClojureScriptProjectTest"]
      (let [output-dirs (into #{} (map file/path) ["build/clojurescript/main"
                                                   "build/clojurescript/test"
                                                   "build/clojure/main"
                                                   "build/clojure/test"
                                                   "build/clojure/dev"])
            classpath-paths (eval-repl client '(do
                                                 (require 'clojure.java.classpath)
                                                 (map str (clojure.java.classpath/classpath))))
            output-dir? (fn [path] (some #(.endsWith path %) output-dirs))]
        (is (not-any? output-dir? (map file/path (edn/read-string classpath-paths))))))))
