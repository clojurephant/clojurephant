pluginManagement {
  plugins {
    id("org.ajoberstar.grgit") version("4.1.0")
    id("org.ajoberstar.reckon") version("0.13.0")
    id("com.diffplug.spotless") version("5.9.0")

    id("dev.clojurephant.clojure") version("0.6.0-alpha.4")
    id("org.ajoberstar.stutter") version("0.6.0")
    id("com.gradle.plugin-publish") version("0.12.0")
  }
}

rootProject.name = "clojurephant"
include("clojurephant-plugin")
include("clojurephant-tools")

// templates
include("templates:clojurephant-clj-lib")
include("templates:clojurephant-clj-app")
include("templates:clojurephant-cljs-app")
