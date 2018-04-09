(ns gradle-clojure.compat-test.incremental-build
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest incremental-build
  (doseq [aot-enabled? [false]]
    (let [aot-compile-opt (format "-DaotCompile=%b" aot-enabled?)
          verify-compilation (if aot-enabled?
                               gradle/verify-compilation-with-aot
                               gradle/verify-compilation-without-aot)]

      (testing (format "aotCompile = %b" aot-enabled?)

        (testing "build is up-to-date when no input changes"
          (gradle/with-project "IncrementalCompilationTest"
            (let [result (gradle/build aot-compile-opt "clean" "classes")]
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
              (verify-compilation "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
              (verify-compilation "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))
            (let [result (gradle/build aot-compile-opt "classes")]
              (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
              (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
              (verify-compilation "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
              (verify-compilation "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))))

        (testing "change in a leaf module"
          (gradle/with-project "IncrementalCompilationTest"
            (let [result (gradle/build aot-compile-opt "clean" "classes")]
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
              (verify-compilation "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
              (verify-compilation "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))

            (file/write-str (gradle/file "moduleB/src/main/clojure/module_b/utils.clj") "(ns module-b.utils) (defn ping [] \"pong\")" :append true)
            (let [result (gradle/build aot-compile-opt "classes")]
              (is (= TaskOutcome/UP_TO_DATE (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
              (verify-compilation "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
              (verify-compilation "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))))

        (testing "change in module used in another module"
          (gradle/with-project "IncrementalCompilationTest"
            (let [result (gradle/build aot-compile-opt "clean" "classes")]
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
              (verify-compilation "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
              (verify-compilation "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))

            (file/write-str (gradle/file "moduleA/src/main/clojure/module_a/utils.clj") "(ns module-a.utils) (defn ping [] \"pong\")" :append true)
            (let [result (gradle/build aot-compile-opt "classes")]
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleA:compileClojure") .getOutcome)))
              (is (= TaskOutcome/SUCCESS (some-> result (.task ":moduleB:compileClojure") .getOutcome)))
              (verify-compilation "moduleA/src/main/clojure" "moduleA/build/classes/clojure/main")
              (verify-compilation "moduleB/src/main/clojure" "moduleB/build/classes/clojure/main"))))))))
