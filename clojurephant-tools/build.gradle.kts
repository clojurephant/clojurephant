import dev.clojurephant.plugin.clojure.tasks.ClojureCheck
import dev.clojurephant.plugin.clojure.tasks.ClojureCompile

plugins {
  `library-convention`
}

dependencies {
  // clojure runtime
  compileOnly("org.clojure:clojure:1.10.1")
  compileOnly("org.clojure:clojurescript:1.10.773")
  compileOnly("nrepl:nrepl:0.8.3")
  compileOnly("com.bhauman:figwheel-main:0.2.12")
  compileOnly("junit:junit:4.13.1")
}

publishing {
  publications {
    create<MavenPublication>("main") {
      from(components["java"])
      artifact(tasks.sourcesJar)
    }
  }
}

tasks.named<ClojureCompile>("compileClojure") {
  // prevent it from trying to compile (since there"s a circular dependency)
  namespaces.set(setOf())
}

tasks.named<ClojureCheck>("checkClojure") {
  // prevent it from trying to compile (since there"s a circular dependency)
  enabled = false
}
