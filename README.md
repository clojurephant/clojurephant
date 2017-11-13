# gradle-clojure

[![Bintray](https://img.shields.io/bintray/v/gradle-clojure/maven/gradle-clojure.svg?style=flat-square)](https://bintray.com/gradle-clojure/maven/gradle-clojure/_latestVersion)
[![Travis](https://img.shields.io/travis/gradle-clojure/gradle-clojure.svg?style=flat-square)](https://travis-ci.org/gradle-clojure/gradle-clojure)
[![GitHub license](https://img.shields.io/github/license/gradle-clojure/gradle-clojure.svg?style=flat-square)](https://github.com/gradle-clojure/gradle-clojure/blob/master/LICENSE)

## What is this?

A Gradle plugin providing support for the Clojure and Clojurescript languages.

**NOTE:** gradle-clojure should not be considered stable until 1.0.0. Until then, minor versions (e.g. 0.1.0 to 0.2.0) will likely contain breaking changes.

### Clojure Features

- Packaging Clojure code (and/or AOT compiled classes) into a JAR
- AOT compilation
- Running clojure.test tests (integrated into Gradle's [Test task](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html))
- Running an nREPL server

### ClojureScript Features

_Coming soon_

## Why should do you care?

The goal is to provide the same creature comforts that [Leiningen](http://leiningen.org/) and [Boot](http://boot-clj.com/) do for Clojure/Clojurescript development, while also leveraging Gradle's unique features:

- Strong support for polyglot projects
- Strong support for multi-project builds
- Large [plugin ecosystem](https://plugins.gradle.org)

## Usage

See the [Release Notes](https://github.com/gradle-clojure/gradle-clojure/releases) for available versions, compatibility with Gradle, Java, and Clojure, and detailed change notes.

This plugin assumes you're using a sane layout for your Clojure code - namespaces corresponding
to your source code layout, and one namespace per file. The plugin uses the filenames to
calculate the namespaces involved, it does not parse the files looking for `ns` forms.

### Quick Start

Download [the sample project](https://github.com/gradle-clojure/gradle-clojure-samples) for the basic structure.

#### Common Commands

- `./gradlew test` Executes your clojure.test tests (and any other JUnit tests in your build).
- `./gradlew clojureRepl` Starts an nREPL server (on a random port by default).

**build.gradle**

```groovy
plugins {
  id "gradle-clojure.clojure" version "<version>"
}

dependencies {
  // whatever version of clojure you prefer (older versions may not be compatible)
  compile 'org.clojure:clojure:1.8.0'
  // and any other dependencies you want on the compile classpath
  // compile 'group:artifact:version'

  // needed for test integration
  testCompile 'junit:junit:4.12'
  // and any other test-specific dependencies
  // testCompile 'group:artifact:version'

  // dependencies for REPL use only
  dev 'org.clojure:tools.namespace:0.3.0-alpha4'
}
```

See all available options in the [docs](docs/README.md).

## Getting help

Please use the repo's [issues](https://github.com/gradle-clojure/gradle-clojure/issues) for all questions, bug reports, and feature requests.

## Contributing

See the [guidelines](.github/CONTRIBUTING.md) for details on how you can contribute.

## Acknowledgements

This project started from the [cursive.clojure](https://github.com/cursive-ide/gradle-clojure) plugin by Colin Fleming (@cmf, original author) and Piotrek Bzdyl (@pbzdyl).

Also thanks to John Szakmeister (@jszakmeister) for organizing a call with Gradle to get us started in the right direction.
