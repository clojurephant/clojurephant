plugins {
  id("com.diffplug.spotless")
}

plugins.withId("java") {
  spotless {
    java {
      importOrder("java", "javax", "")
      removeUnusedImports()
      eclipse().configFile(rootProject.file("gradle/eclipse-java-formatter.xml"))
    }
  }
}
