pluginManagement {
  plugins {
    id("dev.clojurephant.clojure") version("0.8.0-beta.7")

    id("org.ajoberstar.reckon.settings") version("0.19.1")
    id("com.diffplug.spotless") version("7.0.3")
    id("org.ajoberstar.stutter") version("1.0.0")
    id("com.gradle.plugin-publish") version("1.3.1")
  }
}

plugins {
  id("org.ajoberstar.reckon.settings")
}

extensions.configure<org.ajoberstar.reckon.gradle.ReckonExtension> {
  setDefaultInferredScope("patch")
  stages("beta", "rc", "final")
  setScopeCalc(calcScopeFromProp().or(calcScopeFromCommitMessages()))
  setStageCalc(calcStageFromProp())
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
