plugins {
  java
  `maven-publish`
  id("dev.clojurephant.clojure")
  id("com.diffplug.spotless")
}

///////////////////////////////////////////////////////////////////////////////
// Dependencies
///////////////////////////////////////////////////////////////////////////////
repositories {
  mavenCentral()
  maven {
    name = "Clojars"
    url = uri("https://repo.clojars.org/")
  }
}

///////////////////////////////////////////////////////////////////////////////
// Packaging
///////////////////////////////////////////////////////////////////////////////
group = "dev.clojurephant"

configure<JavaPluginConvention> {
  sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.register<Jar>("sourcesJar") {
  from(sourceSets["main"].allSource)
  archiveClassifier.set("sources")
}

///////////////////////////////////////////////////////////////////////////////
// Code Style and Formatting
///////////////////////////////////////////////////////////////////////////////
spotless {
  java {
    importOrder("java", "javax")
    removeUnusedImports()
    eclipse().configFile(rootProject.file("gradle/eclipse-java-formatter.xml"))
  }
  format("gradle") {
    target("**/*.gradle")
    trimTrailingWhitespace()
    indentWithSpaces(2)
    endWithNewline()
  }
}

///////////////////////////////////////////////////////////////////////////////
// Publishing
///////////////////////////////////////////////////////////////////////////////

// Publish to following repositories
publishing {
  repositories {
    maven {
      name = "bintray"
      url = uri("https://api.bintray.com/maven/clojurephant/maven/clojurephant/;publish=1")
      credentials {
        username = System.getenv("BINTRAY_USER")
        password = System.getenv("BINTRAY_KEY")
      }
    }
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
