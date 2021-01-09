plugins {
  id("com.diffplug.spotless")
}

spotless {
  java {
    importOrder("java", "javax", "")
    removeUnusedImports()
    eclipse().configFile(rootProject.file("gradle/eclipse-java-formatter.xml"))
  }
}
