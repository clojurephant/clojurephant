(ns clj.new.gradle-clj-app
  (:require [clj.new.templates :as t]
            [clojure.string :as string]))

(defn unsanitize-ns
  [s]
  (string/replace s "_" "-"))

(defn gradle-clj-app
  [name]
  (let [render (t/renderer "gradle-clj-app")
        raw (t/raw-resourcer "gradle-clj-app")
        main-ns (t/multi-segment (t/sanitize-ns name))
        data {:raw-name name
              :group (t/group-name name)
              :name (t/project-name name)
              :namespace main-ns
              :main-class (unsanitize-ns main-ns)
              :nested-dirs (t/name-to-path main-ns)
              :year (t/year)
              :date (t/date)}]
    (println "Generating a project called"
             (:name data)
             "based on the gradle-clj-app template.")
    (println "The app template is intended for application projects, not libraries.")
    (t/->files data
               ["settings.gradle" (render "settings.gradle" data)]
               ["build.gradle" (render "build.gradle" data)]
               ["gradlew" (render "gradlew" data) :executable true]
               ["gradlew.bat" (render "gradlew.bat" data)]
               ["gradle/wrapper/gradle-wrapper.properties" (render "gradle-wrapper.properties" data)]
               ["gradle/wrapper/gradle-wrapper.jar" (raw "gradle-wrapper.jar")]
               ["README.md" (render "README.md" data)]
               ["doc/intro.md" (render "intro.md" data)]
               [".gitignore" (render "gitignore" data)]
               ["src/main/clojure/{{nested-dirs}}.clj" (render "core.clj" data)]
               ["src/test/clojure/{{nested-dirs}}_test.clj" (render "test.clj" data)]
               ["LICENSE" (render "LICENSE" data)]
               ["CHANGELOG.md" (render "CHANGELOG.md" data)]
               ["src/main/resources/.keep" ""])))
