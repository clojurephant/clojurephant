import dev.clojurephant.plugin.clojure.tasks.ClojureCompile
import org.gradle.plugins.ide.eclipse.model.EclipseModel

plugins {
  id("convention.clojars-publish")
  id("convention.lint")

  id("dev.clojurephant.clojure")

  `java-gradle-plugin`
  id("com.gradle.plugin-publish")
  id("org.ajoberstar.stutter")
}

group = "dev.clojurephant"

configure<JavaPluginConvention> {
  sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  // edn support
  implementation("us.bpsm:edn-java:0.7.1")

  // util
  implementation("org.apache.commons:commons-text:1.9")

  // compat testing
  compatTestImplementation(gradleTestKit())
  compatTestImplementation("org.clojure:clojure:1.10.1")
  compatTestImplementation("org.clojure:tools.namespace:1.1.0")
  compatTestImplementation("nrepl:nrepl:0.8.3")
  compatTestImplementation("junit:junit:4.13.1")
  compatTestImplementation("org.ajoberstar:ike.cljj:0.4.1")
}

stutter {
  setSparse(true)
  java(8) {
    compatibleRange("5.0")
  }
  java(14) {
    compatibleRange("6.3")
  }
}

plugins.withId("eclipse") {
  val eclipse = extensions.getByType(EclipseModel::class)
  eclipse.classpath.plusConfigurations.add(configurations["compatTestCompileClasspath"])
}

sourceSets["compatTest"].runtimeClasspath = files(tasks.compileCompatTestClojure, sourceSets["compatTest"].runtimeClasspath)

tasks.named<ClojureCompile>("compileCompatTestClojure") {
  namespaces.add("dev.clojurephant.tools.logger")
  namespaces.add("dev.clojurephant.tools.clojure-test-junit4")
}

tasks.withType<Test>().matching { t -> t.name.startsWith("compatTest") }.all {
  testClassesDirs = files(tasks.compileCompatTestClojure, sourceSets["compatTest"].output)

  inputs.dir("src/compatTest/projects")
  systemProperty("clojure.test.dirs", "src/compatTest/clojure")
  systemProperty("stutter.projects", "src/compatTest/projects")
  systemProperty("org.gradle.testkit.dir", file("build/stutter-test-kit").absolutePath)
}

gradlePlugin {
  plugins {
    create("clojureBase") {
      id = "dev.clojurephant.clojure-base"
      implementationClass = "dev.clojurephant.plugin.clojure.ClojureBasePlugin"
    }
    create("clojure") {
      id = "dev.clojurephant.clojure"
      implementationClass = "dev.clojurephant.plugin.clojure.ClojurePlugin"
    }
    create("clojurescriptBase") {
      id = "dev.clojurephant.clojurescript-base"
      implementationClass = "dev.clojurephant.plugin.clojurescript.ClojureScriptBasePlugin"
    }
    create("clojurescript") {
      id = "dev.clojurephant.clojurescript"
      implementationClass = "dev.clojurephant.plugin.clojurescript.ClojureScriptPlugin"
    }
  }
}

pluginBundle {
  website = "https://clojurephant.dev/"
  vcsUrl = "https://github.com/clojurephant/clojurephant.git"
  description = "Clojure and ClojureScript language support for Gradle"
  (plugins) {
    "clojureBase" {
      id = "dev.clojurephant.clojure-base"
      displayName = "Clojure base language plugin for Gradle"
      tags = listOf("clojure", "language")
    }
    "clojure" {
      id = "dev.clojurephant.clojure"
      displayName = "Clojure language plugin for Gradle"
      tags = listOf("clojure", "language")
    }
    "clojurescriptBase" {
      id = "dev.clojurephant.clojurescript-base"
      displayName = "ClojureScript base language plugin for Gradle"
      tags = listOf("clojurescript", "language")
    }
    "clojurescript" {
      id = "dev.clojurephant.clojurescript"
      displayName = "ClojureScript language plugin for Gradle"
      tags = listOf("clojurescript", "language")
    }
  }
  mavenCoordinates {
    groupId = project.group.toString()
    artifactId = project.name.toString()
    version = project.version.toString()
  }
}
