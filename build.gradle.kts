import org.gradle.plugins.ide.eclipse.model.EclipseModel

plugins {
  id("dev.clojurephant.clojure")

  id("com.diffplug.spotless")

  id("java-gradle-plugin")
  id("com.gradle.plugin-publish")
  id("org.ajoberstar.stutter")
}

group = "dev.clojurephant"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

dependencies {
  // edn support
  implementation("us.bpsm:edn-java:0.7.1")

  // util
  implementation("org.apache.commons:commons-text:1.11.0")
}

testing {
  suites {
    val compatTest by getting(JvmTestSuite::class) {
      dependencies {
        implementation(gradleTestKit())
        implementation("org.clojure:clojure:1.12.0")
        implementation("org.clojure:tools.namespace:1.5.0")
        implementation("nrepl:nrepl:1.3.1")
        implementation("org.ajoberstar:cljj:0.5.0")
        implementation("org.clojure:data.xml:0.0.8")
        runtimeOnly("dev.clojurephant:jovial:0.4.2")
      }

      targets.all {
        testTask.configure {
          useJUnitPlatform()

          inputs.dir("src/compatTest/projects")
          systemProperty("stutter.projects", "src/compatTest/projects")
          systemProperty("org.gradle.testkit.dir", file("build/stutter-test-kit").absolutePath)
        }
      }
    }
  }
}

stutter {
  val java8 by matrices.creating {
    javaToolchain {
      languageVersion.set(JavaLanguageVersion.of(8))
    }
    gradleVersions {
      compatibleRange("8.0", "9.0")
    }
  }
  val java21 by matrices.creating {
    javaToolchain {
      languageVersion.set(JavaLanguageVersion.of(21))
    }
    gradleVersions {
      compatibleRange("8.6")
    }
  }
}

plugins.withId("eclipse") {
  val eclipse = extensions.getByType(EclipseModel::class)
  eclipse.classpath.plusConfigurations.add(configurations["compatTestCompileClasspath"])
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
      licenses {
        license {
          name.set("Apache-2.0")
          url.set("https://github.com/clojurephant/clojurephant/blob/main/LICENSE")
        }
      }
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

gradlePlugin {
  website.set("https://clojurephant.dev/")
  vcsUrl.set("https://github.com/clojurephant/clojurephant.git")
  plugins {
    create("clojureBase") {
      id = "dev.clojurephant.clojure-base"
      displayName = "Clojure base language plugin for Gradle"
      description = "Clojure base language plugin for Gradle"
      implementationClass = "dev.clojurephant.plugin.clojure.ClojureBasePlugin"
      tags.set(listOf("clojure", "language"))
    }
    create("clojure") {
      id = "dev.clojurephant.clojure"
      displayName = "Clojure language plugin for Gradle"
      description = "Clojure language plugin for Gradle"
      implementationClass = "dev.clojurephant.plugin.clojure.ClojurePlugin"
      tags.set(listOf("clojure", "language"))
    }
    create("clojurescriptBase") {
      id = "dev.clojurephant.clojurescript-base"
      displayName = "ClojureScript base language plugin for Gradle"
      description = "ClojureScript base language plugin for Gradle"
      implementationClass = "dev.clojurephant.plugin.clojurescript.ClojureScriptBasePlugin"
      tags.set(listOf("clojurescript", "language"))
    }
    create("clojurescript") {
      id = "dev.clojurephant.clojurescript"
      displayName = "ClojureScript language plugin for Gradle"
      description = "ClojureScript language plugin for Gradle"
      implementationClass = "dev.clojurephant.plugin.clojurescript.ClojureScriptPlugin"
      tags.set(listOf("clojurescript", "language"))
    }
  }
}

spotless {
  java {
    importOrder("java", "javax", "")
    removeUnusedImports()
    eclipse().configFile(rootProject.file("gradle/eclipse-java-formatter.xml"))
  }
}

// accept build scan terms
extensions.findByName("buildScan")?.withGroovyBuilder {
  setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
  setProperty("termsOfServiceAgree", "yes")
}
