# gradle-clojure

[![Travis](https://img.shields.io/travis/gradle-clojure/gradle-clojure.svg?style=flat-square)](https://travis-ci.org/gradle-clojure/gradle-clojure)
[![GitHub license](https://img.shields.io/github/license/gradle-clojure/gradle-clojure.svg?style=flat-square)](https://github.com/gradle-clojure/gradle-clojure/blob/master/LICENSE)

## What is this?

A Gradle plugin providing support for the Clojure and Clojurescript languages.

### Current Features

- Clojure compilation
- Packaging Clojure code (or AOT compiled classes) into a JAR
- Running clojure.test tests

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

This plugin currently only implements compilation and test running. More features may be added,
but features provided by Gradle itself will not be (uberjarring, project publishing). I don't
use those features myself, examples of build script snippets to perform them for the doc would
be very welcome.

There is currently no functionality for running a REPL - you'll need to run an application which
starts an nREPL server, or something similar.

### Quick Start

```groovy
plugins {
  id "gradle-clojure.clojure" version "<version>"
}

compileClojure {
  aotCompile = true            // Defaults to false
  copySourceToOutput = false   // Defaults to !aotCompile

  reflectionWarnings {
    enabled = true             // Defaults to false
    projectOnly = true         // Only show warnings from your project, not dependencies - default false
    asErrors = true            // Treat reflection warnings as errors and fail the build
                               // If projectOnly is true, only warnings from your project are errors.
  }

  // Compiler options for AOT
  disableLocalsClearing = true                 // Defaults to false
  elideMeta = ['doc', 'file', 'line', 'added'] // Defaults to []
  directLinking = true                         // Defaults to false

  // compileClojure implements the standard JavaForkOptions interface, and thus supports the
  // standard Gradle mechanisms for configuring a Java process:
  // systemProperty systemProperties minHeapSize maxHeapSize
  // jvmArgs bootstrapClasspath classpath enableAssertions debug environment

  systemProperty 'java.awt.headless', true
  maxHeapSize '2048m'
  jvmArgs '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005'
}

compileTestClojure {
  // compileTestClojure accepts the same options as compileClojure, but you're unlikely to AOT
  // compile your tests

  // Select the files for testing using the standard Gradle include/exclude mechanisms
  exclude 'cursive/**/*generative*'
}

testClojure {
  // Standard JVM execution options here for test process
  systemProperty 'java.awt.headless', true

  // Specifying junitReport will trigger JUnit XML report generation
  // in addition to standard console output (turned off by default)
  junitReport = file("$buildDir/reports/junit-report.xml")
}
```

## Getting help

Please use the repo's [issues](https://github.com/gradle-clojure/gradle-clojure/issues) for all questions, bug reports, and feature requests.

## Contributing

See the [guidelines](.github/CONTRIBUTING.md) for details on how you can contribute.

## Acknowledgements

This project started from the [cursive.clojure](https://github.com/cursive-ide/gradle-clojure) plugin by Colin Fleming (@cmf, original author) and Piotrek Bzdyl (@pbzdyl).

Also thanks to John Szakmeister (@jszakmeister) for organizing a call with Gradle to get us started in the right direction.
