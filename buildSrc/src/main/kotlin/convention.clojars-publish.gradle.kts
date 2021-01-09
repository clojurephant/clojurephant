plugins {
  java
  `maven-publish`
}

java {
  withSourcesJar()
}

publishing {
  repositories {
    maven {
      name = "clojars"
      url = uri("https://repo.clojars.org")
      credentials {
        username = System.getenv("CLOJARS_USER")
        password = System.getenv("CLOJARS_TOKEN")
      }
    }
  }

  publications.withType<MavenPublication>() {
    // use static versions in poms
    versionMapping {
      usage("java-api") {
        fromResolutionOf("runtimeClasspath")
      }
      usage("java-runtime") {
        fromResolutionResult()
      }
    }

    pom {
      // include repository info in POM (needed for cljdoc)
      scm {
        connection.set("https://github.com/clojurephant/clojurephant.git")
        developerConnection.set("git@github.com:clojurephant/clojurephant.git")
        url.set("https://github.com/clojurephant/clojurephant")
        if (!version.toString().contains("+")) {
          tag.set(version.toString())
        }
      }
    }
  }
}

// Clojars doesn"t support module metadata
tasks.withType<GenerateModuleMetadata>() {
    enabled = false
}
