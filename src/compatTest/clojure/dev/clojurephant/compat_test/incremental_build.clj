(ns dev.clojurephant.compat-test.incremental-build
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [dev.clojurephant.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file])
  (:import [org.gradle.testkit.runner TaskOutcome]))

(deftest leaf-with-aot
  (testing "change in a leaf module"
    (gradle/with-project "IncrementalCompilationTest"
      (file/write-str (gradle/file "moduleA/build.gradle") "clojure { builds { main { aotAll() } } }\n" :append true)
      (file/write-str (gradle/file "moduleB/build.gradle") "clojure { builds { main { aotAll() } } }\n" :append true)
      (let [result (gradle/build "clean" "classes")]
        (gradle/verify-task-outcome result ":moduleA:compileClojure" :success)
        (gradle/verify-task-outcome result ":moduleB:compileClojure" :success)
        (gradle/verify-compilation-with-aot "moduleA/src/main/clojure" "moduleA/build/clojure/main")
        (gradle/verify-compilation-with-aot "moduleB/src/main/clojure" "moduleB/build/clojure/main"))

      (file/write-str (gradle/file "moduleB/src/main/clojure/module_b/utils.clj") "(ns module-b.utils) (defn ping [] \"pong\")")
      (let [result (gradle/build "classes")]
        (gradle/verify-task-outcome result ":moduleA:compileClojure" :up-to-date)
        (gradle/verify-task-outcome result ":moduleB:compileClojure" :success)
        (gradle/verify-compilation-with-aot "moduleA/src/main/clojure" "moduleA/build/clojure/main")
        (gradle/verify-compilation-with-aot "moduleB/src/main/clojure" "moduleB/build/clojure/main")))))

(deftest leaf-no-aot
  (testing "change in a leaf module"
    (gradle/with-project "IncrementalCompilationTest"
      (let [result (gradle/build "clean" "check")]
        (gradle/verify-task-outcome result ":moduleA:checkClojure" :success)
        (gradle/verify-task-outcome result ":moduleB:checkClojure" :success))

      (file/write-str (gradle/file "moduleB/src/main/clojure/module_b/utils.clj") "(ns module-b.utils) (defn ping [] \"pong\")")
      (let [result (gradle/build "check")]
        (gradle/verify-task-outcome result ":moduleA:checkClojure" :up-to-date)
        (gradle/verify-task-outcome result ":moduleB:checkClojure" :success)))))

(deftest module-with-aot
  (testing "change in module used in another module"
    (gradle/with-project "IncrementalCompilationTest"
      (file/write-str (gradle/file "moduleA/build.gradle") "clojure { builds { main { aotAll() } } }\n" :append true)
      (file/write-str (gradle/file "moduleB/build.gradle") "clojure { builds { main { aotAll() } } }\n" :append true)
      (let [result (gradle/build "clean" "classes")]
        (gradle/verify-task-outcome result ":moduleA:compileClojure" :success)
        (gradle/verify-task-outcome result ":moduleB:compileClojure" :success)
        (gradle/verify-compilation-with-aot "moduleA/src/main/clojure" "moduleA/build/clojure/main")
        (gradle/verify-compilation-with-aot "moduleB/src/main/clojure" "moduleB/build/clojure/main"))

      (file/write-str (gradle/file "moduleA/src/main/clojure/module_a/utils.clj") "(ns module-a.utils) (defn ping [] \"pong\")")
      (let [result (gradle/build "classes")]
        (gradle/verify-task-outcome result ":moduleA:compileClojure" :success)
        (gradle/verify-task-outcome result ":moduleB:compileClojure" :success)
        (gradle/verify-compilation-with-aot "moduleA/src/main/clojure" "moduleA/build/clojure/main")
        (gradle/verify-compilation-with-aot "moduleB/src/main/clojure" "moduleB/build/clojure/main")))))

(deftest module-no-aot
  (testing "change in module used in another module"
    (gradle/with-project "IncrementalCompilationTest"
      (let [result (gradle/build "clean" "check")]
        (gradle/verify-task-outcome result ":moduleA:checkClojure" :success)
        (gradle/verify-task-outcome result ":moduleB:checkClojure" :success))

      (file/write-str (gradle/file "moduleA/src/main/clojure/module_a/utils.clj") "(ns module-a.utils) (defn ping [] \"pong\")")
      (let [result (gradle/build "check")]
        (gradle/verify-task-outcome result ":moduleA:checkClojure" :success)
        (gradle/verify-task-outcome result ":moduleB:checkClojure" :success)))))

(deftest no-change-with-aot
  (testing "build is up-to-date when no input changes"
    (gradle/with-project "IncrementalCompilationTest"
      (file/write-str (gradle/file "moduleA/build.gradle") "clojure { builds { main { aotAll() } } }\n" :append true)
      (file/write-str (gradle/file "moduleB/build.gradle") "clojure { builds { main { aotAll() } } }\n" :append true)
      (let [result (gradle/build "clean" "classes")]
        (gradle/verify-task-outcome result ":moduleA:compileClojure" :success)
        (gradle/verify-task-outcome result ":moduleB:compileClojure" :success)
        (gradle/verify-compilation-with-aot "moduleA/src/main/clojure" "moduleA/build/clojure/main")
        (gradle/verify-compilation-with-aot "moduleB/src/main/clojure" "moduleB/build/clojure/main"))
      (let [result (gradle/build "classes")]
        (gradle/verify-task-outcome result ":moduleA:compileClojure" :up-to-date)
        (gradle/verify-task-outcome result ":moduleB:compileClojure" :up-to-date)))))

(deftest no-change-no-aot
  (testing "build is up-to-date when no input changes"
    (gradle/with-project "IncrementalCompilationTest"
      (let [result (gradle/build "clean" "check")]
        (gradle/verify-task-outcome result ":moduleA:checkClojure" :success)
        (gradle/verify-task-outcome result ":moduleB:checkClojure" :success))

      (let [result (gradle/build "check")]
        (gradle/verify-task-outcome result ":moduleA:checkClojure" :up-to-date)
        (gradle/verify-task-outcome result ":moduleB:checkClojure" :up-to-date)))))
