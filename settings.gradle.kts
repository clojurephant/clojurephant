pluginManagement {
  plugins {
    id("org.ajoberstar.grgit") version("5.0.0")
    id("org.ajoberstar.reckon") version("0.16.1")
    id("com.diffplug.spotless") version("6.7.2")

    id("dev.clojurephant.clojure") version("0.7.0-alpha.1")
    id("org.ajoberstar.stutter") version("0.7.1")
    id("com.gradle.plugin-publish") version("1.0.0")
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

rootProject.name = "clojurephant-plugin"
