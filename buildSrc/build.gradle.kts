plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
  maven(url = "https://repo.clojars.org")
}

dependencies {
  implementation("dev.clojurephant:clojurephant-plugin:0.6.0-beta.1")
  implementation("com.diffplug.spotless:spotless-plugin-gradle:5.9.0")
}
