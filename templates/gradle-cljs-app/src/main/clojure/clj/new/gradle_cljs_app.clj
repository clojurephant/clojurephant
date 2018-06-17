(ns clj.new.gradle-cljs-app
  (:require [clj.new.templates :as t]))

(defn gradle-cljs-app
  [name]
  (let [render (t/renderer "gradle-cljs-app")
        raw (t/raw-resourcer "gradle-cljs-app")
        main-ns (t/multi-segment (t/sanitize-ns name))
        data {:raw-name name
              :group (t/group-name name)
              :name (t/project-name name)
              :namespace main-ns
              :nested-dirs (t/name-to-path main-ns)
              :year (t/year)
              :date (t/date)}]
    (println "Generating a project called"
             (:name data)
             "based on the gradle-cljs-app template.")
    (println "The app template is intended for ClojureScript applications.")
    (println "To get started:")
    (println "  1) Run \"./gradlew clojureRepl\" to start an nREPL server.")
    (println "  2) Connect to the nREPL server in your favorite editor (see the port in the output).")
    (println "  3) Enter \"(require '[gradle-clojure.tools.figwheel :as fw])\" in the REPL.")
    (println "  4) Enter \"(fw/start \"dev\")\" in the REPL.")
    (println "  5) Your browser should automatically open to the Figwheel server.")
    (t/->files data
               ["settings.gradle" (render "settings.gradle" data)]
               ["build.gradle" (render "build.gradle" data)]
               ["gradlew" (render "gradlew" data) :executable true]
               ["gradlew.bat" (render "gradlew.bat" data)]
               ["gradle/wrapper/gradle-wrapper.properties" (render "gradle-wrapper.properties" data)]
               ["gradle/wrapper/gradle-wrapper.jar" (raw "gradle-wrapper.jar")]

               [".gitignore" (render "gitignore" data)]
               ["README.md" (render "README.md" data)]
               ["doc/intro.md" (render "intro.md" data)]
               ["LICENSE" (render "LICENSE" data)]
               ["CHANGELOG.md" (render "CHANGELOG.md" data)]

               ["src/main/clojure/{{nested-dirs}}.clj" (render "core.clj" data)]
               ["src/main/clojurescript/{{nested-dirs}}.cljs" (render "core.cljs" data)]
               ["src/dev/clojurescript/{{nested-dirs}}/dev.cljs" (render "dev.cljs" data)]
               ["src/main/resources/public/index.html" (render "index.html" data)]
               ["src/main/resources/public/css/style.css" (render "style.css" data)])))
