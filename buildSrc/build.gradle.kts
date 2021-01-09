plugins {
  `kotlin-dsl`
}

repositories {
  jcenter()
  maven(url = "https://repo.clojars.org")
}

dependencies {
  implementation("dev.clojurephant:clojurephant-plugin:0.6.0-alpha.4")
  implementation("com.diffplug.spotless:spotless-plugin-gradle:5.9.0")
}
