# New Gradle Users

**NOTE** This example assumes Gradle 4 or higher.

[Gradle](https://docs.gradle.org/4.5.1/userguide/userguide.html) is a build automation tool in the same space as Maven, Leiningen, and Boot. Gradle is primarily targeted at projects using the JVM, but has plugins for many other languages. (Now including Clojure!)

## Installing Gradle

See [Gradle's installation documentation](https://docs.gradle.org/4.5.1/userguide/installation.html).

## Initializing a project

```
$ mkdir my-project
$ cd my-project
$ gradle init
```

> From here on out you'll use `./gradlew` instead of `gradle` in your commands. `gradlew` is the [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) which allows you to set a per-project Gradle version. This ensures all developers use the same Gradle version for the project, instead of whatever happens to be on their `PATH`.

Also see [Gradle's Creating New Gradle Builds](https://guides.gradle.org/creating-new-gradle-builds).

## Adding the plugin

To include plugins from [Gradle's Plugin Portal](https://plugins.gradle.org/) you'll use a `plugins {}` block. This should be at the top of your `build.gradle`

```groovy
plugins {
  id 'gradle-clojure.clojure' version '0.3.1'
  // any additional plugins declared here
}
```

Also see [Gradle's Using Plugins](https://docs.gradle.org/4.5.1/userguide/plugins.html).

## Configuring project information

```groovy
group = 'my.example' // the group ID your artifacts should be published under
version = '0.1.0-SNAPSHOT' // the version your artifacts should be published under
```

## Putting it all together

Full `build.gradle` example:

```groovy
plugins {
  id 'gradle-clojure.clojure' version '0.3.1'
}

group = 'my.example'
version = '0.1.0-SNAPSHOT'

repositories {
  mavenCentral()
}

dependencies {
  compile 'org.clojure:clojure:1.9.0'

  testCompile 'junit:junit:4.12'

  devCompile 'org.clojure:tools.namespace:0.3.0-alpha4'
}
```
