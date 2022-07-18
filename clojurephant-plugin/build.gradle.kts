import dev.clojurephant.plugin.clojure.tasks.ClojureCompile
import org.gradle.plugins.ide.eclipse.model.EclipseModel

plugins {
  id("convention.clojars-publish")
  id("com.diffplug.spotless")

  id("dev.clojurephant.clojure")

  `java-gradle-plugin`
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
  implementation("org.apache.commons:commons-text:1.9")

  // compat testing
  compatTestImplementation(gradleTestKit())
  compatTestImplementation("org.clojure:clojure:1.11.1")
  compatTestImplementation("org.clojure:tools.namespace:1.3.0")
  compatTestImplementation("nrepl:nrepl:0.9.0")
  compatTestImplementation("org.ajoberstar:ike.cljj:0.4.1")
  compatTestRuntimeOnly("org.ajoberstar:jovial:0.3.0")
}

stutter {
  val java8 by matrices.creating {
    javaToolchain {
      languageVersion.set(JavaLanguageVersion.of(8))
    }
    gradleVersions {
      compatibleRange("6.4")
    }
  }
  val java11 by matrices.creating {
    javaToolchain {
      languageVersion.set(JavaLanguageVersion.of(11))
    }
    gradleVersions {
      compatibleRange("6.4")
    }
  }
  val java17 by matrices.creating {
    javaToolchain {
      languageVersion.set(JavaLanguageVersion.of(17))
    }
    gradleVersions {
      compatibleRange("7.3")
    }
  }
}

plugins.withId("eclipse") {
  val eclipse = extensions.getByType(EclipseModel::class)
  eclipse.classpath.plusConfigurations.add(configurations["compatTestCompileClasspath"])
}

tasks.withType<Test>() {
  useJUnitPlatform()
}

tasks.withType<Test>() {
  inputs.dir("src/compatTest/projects")
  systemProperty("stutter.projects", "src/compatTest/projects")
  systemProperty("org.gradle.testkit.dir", file("build/stutter-test-kit").absolutePath)
}

gradlePlugin {
  plugins {
    create("clojureBase") {
      id = "dev.clojurephant.clojure-base"
      displayName = "Clojure base language plugin for Gradle"
      implementationClass = "dev.clojurephant.plugin.clojure.ClojureBasePlugin"
    }
    create("clojure") {
      id = "dev.clojurephant.clojure"
      displayName = "Clojure language plugin for Gradle"
      implementationClass = "dev.clojurephant.plugin.clojure.ClojurePlugin"
    }
    create("clojurescriptBase") {
      id = "dev.clojurephant.clojurescript-base"
      displayName = "ClojureScript base language plugin for Gradle"
      implementationClass = "dev.clojurephant.plugin.clojurescript.ClojureScriptBasePlugin"
    }
    create("clojurescript") {
      id = "dev.clojurephant.clojurescript"
      displayName = "ClojureScript language plugin for Gradle"
      implementationClass = "dev.clojurephant.plugin.clojurescript.ClojureScriptPlugin"
    }
  }
}

pluginBundle {
  website = "https://clojurephant.dev/"
  vcsUrl = "https://github.com/clojurephant/clojurephant.git"
  description = "Clojure and ClojureScript language support for Gradle"
  tags = listOf("clojure", "clojurescript", "language")
}

spotless {
  java {
    importOrder("java", "javax", "")
    eclipse().configFile(rootProject.file("gradle/eclipse-java-formatter.xml"))
  }
}
