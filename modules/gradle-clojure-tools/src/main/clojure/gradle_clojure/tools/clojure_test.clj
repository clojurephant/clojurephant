;
; Copyright 2016 Colin Fleming
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
; http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;

(ns gradle-clojure.tools.clojure-test
  (:require
    [clojure.java.io]
    [clojure.test]
    [clojure.test.junit])
  (:import (java.io File)))

(let [plain-report clojure.test/report
      junit-report clojure.test.junit/junit-report]
  (defn- junit-logging-report-fn [plain-test-out]
    (fn [m]
      (junit-report m)
      (binding [clojure.test/*test-out* plain-test-out]
        (plain-report m)))))

(defn- run-tests-with-junit-report
  [^File junit-report-filename & ns-syms]

  (when-let [report-dir (.getParentFile junit-report-filename)]
    (.mkdirs report-dir))

  (let [plain-test-out clojure.test/*test-out*]
    (with-open [xml-test-out (clojure.java.io/writer junit-report-filename)]
      (binding [clojure.test/*test-out* xml-test-out
                clojure.test.junit/junit-report (junit-logging-report-fn plain-test-out)]
        (clojure.test.junit/with-junit-output
          (apply clojure.test/run-tests ns-syms))))))

(defn run-tests*
  [ns-syms runner-fn]
  (apply require ns-syms)
  (let [summary (apply runner-fn ns-syms)]
    (if-not (clojure.test/successful? summary)
      (throw (ex-info (str "One or more tests failed: " summary) {})))))

(defn run-tests [ns-names ^File junit-report-filename]
  (let [ns-syms (map symbol ns-names)]
    (if junit-report-filename
      (run-tests* ns-syms (partial run-tests-with-junit-report junit-report-filename))
      (run-tests* ns-syms clojure.test/run-tests))))
