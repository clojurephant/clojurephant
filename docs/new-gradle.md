# New Gradle Users

{% include nav.md %}

**NOTE:** This example assumes Gradle 6 or higher.

[Gradle](https://docs.gradle.org/current/userguide/userguide.html) is a build automation tool in the same space as [Maven](https://maven.apache.org), [Leiningen](https://leiningen.org), and [Boot](https://boot-clj.com). Gradle is primarily targeted at projects using the JVM, but has plugins for many other languages. (Now including Clojure!)

## Installing Gradle

See [Gradle's installation documentation](https://docs.gradle.org/current/userguide/installation.html).

## Initializing a project

```
$ mkdir my-project
$ cd my-project
$ gradle init
```

> From here on out you'll use `./gradlew` instead of `gradle` in your commands. `gradlew` is the [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) which allows you to set a per-project Gradle version. This ensures all developers use the same Gradle version for the project, instead of whatever happens to be on their `PATH`.

Also see [Gradle's Learning the Basics](https://docs.gradle.org/current/userguide/tutorial_using_tasks.html).

## Adding the plugin

To include plugins from [Gradle's Plugin Portal](https://plugins.gradle.org/) you'll use a `plugins {}` block. This should be at the top of your `build.gradle`.

```groovy
plugins {
  id 'dev.clojurephant.clojure' version '0.7.0-alpha.1'
  // any additional plugins declared here
}
```

Also see [Gradle's Using Plugins](https://docs.gradle.org/current/userguide/plugins.html).

## Configuring project information

```groovy
group = 'my.example' // the group ID your artifacts should be published under
version = '0.1.0-SNAPSHOT' // the version your artifacts should be published under
```

## Define dependencies

See [Gradle's Dependency Management docs](https://docs.gradle.org/current/userguide/core_dependency_management.html).

### Repositories

No repositories are specified by default, so you must list any repositories you want to search for your dependencies.

**IMPORTANT:** clojurephant currently requires Clojars be included in your repository list.

```groovy
repositories {
  maven {
    name = 'Clojars' // name can be ommitted, but is helpful in troubleshooting
    url = 'https://repo.clojars.org/'
  }
}
```

Also see [Gradle's Declaring Repositories](https://docs.gradle.org/current/userguide/declaring_repositories.html).

## Dependencies

Unless you have a reason to do otherwise, use Gradle's shorthand syntax `<configuration> '<group>:<artifact>:<version>'` (e.g. `compile 'org.clojure:clojure:1.9.0'`) to specify dependencies.

Dependencies are put in different configurations (somewhat similar to Maven scopes). For Clojure's purposes, the three main ones to be aware of are:

- `implementation` - dependencies of your main application code
- `testImplementation` - dependencies of your test code
- `devImplementation` - dependencies used only in the REPL

```groovy
dependencies {
  implementation 'org.clojure:clojure:1.11.1'

  // due to how clojure.test is executed, a JUnit test engine (Jovial) is needed
  testRuntimeOnly 'org.ajoberstar:jovial:0.3.0'

  // due to the way Gradle's REPL is started, if you need tools.namespace, you must be on 0.3+
  devImplementation 'org.clojure:tools.namespace:1.3.0'
}

// due to how clojure.test is executed, the JUnit platform is needed
tasks.withType(Test) {
  useJUnitPlatform()
}

```

Also see [Gradle's Declaring Dependencies](https://docs.gradle.org/current/userguide/declaring_dependencies.html)

## Putting it all together

Full `build.gradle` example:

```groovy
plugins {
  id 'dev.clojurephant.clojure' version '0.7.0-alpha.1'
}

group = 'my.example'
version = '0.1.0-SNAPSHOT'

repositories {
  jcenter()
}

dependencies {
  implementation 'org.clojure:clojure:1.11.1'

  testRuntimeOnly 'org.ajoberstar:jovial:0.3.0'

  devImplementation 'org.clojure:tools.namespace:1.3.0'
}

tasks.withType(Test) {
  useJUnitPlatform()
}
```
