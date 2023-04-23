pluginManagement {
  plugins {
    id("dev.clojurephant.clojure") version("0.7.0")

    id("org.ajoberstar.reckon.settings") version("0.18.0")
    id("com.diffplug.spotless") version("6.18.0")
    id("org.ajoberstar.stutter") version("0.7.2")
    id("com.gradle.plugin-publish") version("1.2.0")
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
