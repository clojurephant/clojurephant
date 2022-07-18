plugins {
  `kotlin-dsl`
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

repositories {
  mavenCentral()
  maven(url = "https://repo.clojars.org")
}

dependencies {
  implementation("dev.clojurephant:clojurephant-plugin:0.7.0-alpha.1")
}
