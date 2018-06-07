(ns gradle-clojure.compat-test.incremental-build
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest leaf-with-aot
  (testing "change in a leaf module"
    (gradle/with-project "IncrementalCompilationTest"
      (let [result (gradle/build "clean" "classesAot")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
        (gradle/verify-compilation-with-aot "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
        (gradle/verify-compilation-with-aot "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))

      (file/write-str (gradle/file "moduleB/src/main/clojure/module_b/utils.clj") "(ns module-b.utils) (defn ping [] \"pong\")")
      (let [result (gradle/build "classesAot")]
        (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
        (gradle/verify-compilation-with-aot "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
        (gradle/verify-compilation-with-aot "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main")))))

(deftest leaf-no-aot
  (testing "change in a leaf module"
    (gradle/with-project "IncrementalCompilationTest"
      (let [result (gradle/build "clean" "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:checkClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:checkClojure") .getOutcome))))

      (file/write-str (gradle/file "moduleB/src/main/clojure/module_b/utils.clj") "(ns module-b.utils) (defn ping [] \"pong\")")
      (let [result (gradle/build "check")]
        (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":moduleA:checkClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:checkClojure") .getOutcome)))))))

(deftest module-with-aot
  (testing "change in module used in another module"
    (gradle/with-project "IncrementalCompilationTest"
      (let [result (gradle/build "clean" "classesAot")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
        (gradle/verify-compilation-with-aot "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
        (gradle/verify-compilation-with-aot "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))

      (file/write-str (gradle/file "moduleA/src/main/clojure/module_a/utils.clj") "(ns module-a.utils) (defn ping [] \"pong\")")
      (let [result (gradle/build "classesAot")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
        (gradle/verify-compilation-with-aot "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
        (gradle/verify-compilation-with-aot "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main")))))

(deftest module-no-aot
  (testing "change in module used in another module"
    (gradle/with-project "IncrementalCompilationTest"
      (let [result (gradle/build "clean" "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:checkClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:checkClojure") .getOutcome))))

      (file/write-str (gradle/file "moduleA/src/main/clojure/module_a/utils.clj") "(ns module-a.utils) (defn ping [] \"pong\")")
      (let [result (gradle/build "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:checkClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:checkClojure") .getOutcome)))))))

(deftest no-change-with-aot
  (testing "build is up-to-date when no input changes"
    (gradle/with-project "IncrementalCompilationTest"
      (let [result (gradle/build "clean" "classesAot")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
        (gradle/verify-compilation-with-aot "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
        (gradle/verify-compilation-with-aot "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))
      (let [result (gradle/build "classesAot")]
        (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
        (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":moduleB:compileClojure") .getOutcome)))))))

(deftest no-change-no-aot
  (testing "build is up-to-date when no input changes"
    (gradle/with-project "IncrementalCompilationTest"
      (let [result (gradle/build "clean" "check")]
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:checkClojure") .getOutcome)))
        (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:checkClojure") .getOutcome))))

      (let [result (gradle/build "check")]
        (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":moduleA:checkClojure") .getOutcome)))
        (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":moduleB:checkClojure") .getOutcome)))))))
