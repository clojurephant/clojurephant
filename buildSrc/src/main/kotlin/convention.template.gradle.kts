import java.io.ByteArrayOutputStream
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.util.GradleVersion

plugins {
  id("dev.clojurephant.clojure")
  id("maven-publish")
}

dependencies {
  compileOnly("org.clojure:clojure:1.10.1")
  compileOnly("seancorfield:clj-new:1.1.234")
}

publishing {
  publications {
    create<MavenPublication>("main") {
      group = project.name
      artifactId = "clj-template"
      from(components["java"])
    }
  }
}

tasks.named<Copy>("processResources") {
  eachFile {
    if (name.endsWith(".gradle")) {
      filter(ReplaceTokens::class, "tokens" to mapOf("clojurephant.version" to project.version.toString()))
    }
  }
}

tasks.register<JavaExec>("newProject") {
  workingDir = file("${rootProject.projectDir}/templates-test")
  classpath = files(sourceSets["main"].output, configurations["compileClasspath"])
  main = "clojure.main"
  args("-m", "clj-new.create", project.name, "my.group/sample-${project.name}", "+localplugin")
  doFirst {
    workingDir.mkdirs()
    delete("${workingDir}/sample-${project.name}")
  }
  dependsOn(tasks.jar)
  dependsOn(":clojurephant-tools:publishToMavenLocal")
  dependsOn(":clojurephant-plugin:publishToMavenLocal")
}

tasks.register<Exec>("verifyGradleVersion") {
  dependsOn(tasks.named("newProject"))

  workingDir = file("${rootProject.projectDir}/templates-test/sample-${project.name}")
  if (System.getProperty("os.name").toLowerCase().contains("windows")) {
    executable = "gradlew.bat"
  } else {
    executable = "./gradlew"
  }
  args("--version")

  standardOutput = ByteArrayOutputStream()
  doLast {
    val out = standardOutput.toString()
    val versionPattern = """Gradle (\d+\S+)""".toRegex()
    val matchResult = versionPattern.find(out)
    val (version) = matchResult!!.destructured
    if (!GradleVersion.version(version).equals(GradleVersion.current())) {
      throw GradleException("Templates have incorrect Gradle wrapper \"$version\". Run ./copy-wrapper.ps1 to update.")
    }
  }
}

tasks.named("check") {
  dependsOn(tasks.named("verifyGradleVersion"))
}
