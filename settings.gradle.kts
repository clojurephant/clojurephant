pluginManagement {
  plugins {
    id("org.ajoberstar.grgit") version("4.1.0")
    id("org.ajoberstar.reckon") version("0.13.0")
    id("com.diffplug.spotless") version("5.15.0")

    id("dev.clojurephant.clojure") version("0.6.0")
    id("org.ajoberstar.stutter") version("0.6.0")
    id("com.gradle.plugin-publish") version("0.15.0")
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven {
      name = "Clojars"
      url = uri("https://repo.clojars.org/")
    }
  }
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

rootProject.name = "clojurephant"
include("clojurephant-plugin")

// templates
include("templates:clojurephant-clj-lib")
include("templates:clojurephant-clj-app")
include("templates:clojurephant-cljs-app")
