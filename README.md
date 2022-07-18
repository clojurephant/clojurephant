# clojurephant

Formerly known as "gradle-clojure"

[![CI](https://github.com/clojurephant/clojurephant/actions/workflows/ci.yaml/badge.svg)](https://github.com/clojurephant/clojurephant/actions/workflows/ci.yaml)
[![cljdoc](https://cljdoc.org/badge/dev.clojurephant/clojurephant-plugin)](https://cljdoc.org/d/dev.clojurephant/clojurephant-plugin/CURRENT)

## What is this?

A Gradle plugin providing support for the Clojure and ClojureScript languages.

**NOTE:** clojurephant should not be considered stable until 1.0.0. Until then, minor versions (e.g. 0.1.0 to 0.2.0) will likely contain breaking changes.

### Clojure Features

- Packaging Clojure code (and/or AOT compiled classes) into a JAR
- Package an Uberjar (via the Gradle [Shadow plugin](http://imperceptiblethoughts.com/shadow/))
- AOT compilation
- Running clojure.test tests (integrated into Gradle's [Test task](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html))
- Running an nREPL server (supports custom middlewares or handler)

### ClojureScript Features

**NOTE:** ClojureScript features are pretty new, so let us know if you run into issues or have ideas for improvement.

- Packaging Clojure code (or compiled JS) into a JAR or ZIP
- ClojureScript compilation (multiple builds supported)

## Why should you care?

The goal is to provide the same creature comforts that [Leiningen](http://leiningen.org/) and [Boot](http://boot-clj.com/) do for Clojure/ClojureScript development, while also leveraging Gradle's unique features:

- Strong support for polyglot projects
- Strong support for multi-project builds
- Large [plugin ecosystem](https://plugins.gradle.org)

## Usage

See the [Release Notes](https://github.com/clojurephant/clojurephant/releases) for available versions, compatibility with Gradle, Java, and Clojure, and detailed change notes.

This plugin assumes you're using a sane layout for your Clojure code - namespaces corresponding
to your source code layout, and one namespace per file. The plugin uses the filenames to
calculate the namespaces involved, it does not parse the files looking for `ns` forms.

### Quick Start

Look at our sample projects:

- [Clojure Library](https://github.com/clojurephant/sample-clojurephant-clj-lib)
- [Clojure Application](https://github.com/clojurephant/sample-clojurephant-clj-app)
- [ClojureScript Application](https://github.com/clojurephant/sample-clojurephant-cljs-app)

#### Common Commands

- `./gradlew test` Executes your clojure.test tests (and any other JUnit tests in your build).
- `./gradlew clojureRepl` Starts an nREPL server (on a random port by default).

**build.gradle**

```groovy
plugins {
  id "dev.clojurephant.clojure" version "<version>"
}

// You need to add clojars for the plugin to work.
repositories {
  maven {
    name = 'Clojars' // name can be ommitted, but is helpful in troubleshooting
    url = 'https://repo.clojars.org/'
  }
}

dependencies {
  // requires a version of Clojure with prepl (1.10+)
  implementation 'org.clojure:clojure:1.11.1'
  // and any other dependencies you want on the compile classpath
  // implementation 'group:artifact:version'

  // needed for test integration
  testRuntimeOnly 'org.ajoberstar:jovial:0.3.0'
  // and any other test-specific dependencies
  // testImplementation 'group:artifact:version'

  // dependencies for REPL use only
  devImplementation 'org.clojure:tools.namespace:1.3.0'
}

// needed for test integration
tasks.withType(Test) {
  useJUnitPlatform()
}

```

See all available options in the [docs](https://clojurephant.dev).

## Getting help

Read the online Clojurephant documentation [https://clojurephant.dev](https://clojurephant.dev).

For questions or support, please visit the [Clojurephant Discussions](https://github.com/clojurephant/clojurephant/discussions), [ClojureVerse gradle-clojure channel](https://clojureverse.org/c/projects/gradle-clojure) or the [Clojurian's Slack #gradle channel](http://clojurians.net/)

For bug reports and feature requests, please use the repo's [issues](https://github.com/clojurephant/clojurephant/issues).

## Contributing

See the [guidelines](.github/CONTRIBUTING.md) for details on how you can contribute.

## Acknowledgements

This project started from the [cursive.clojure](https://github.com/cursive-ide/gradle-clojure) plugin by Colin Fleming (@cmf, original author) and Piotrek Bzdyl (@pbzdyl).

Thanks to John Szakmeister (@jszakmeister) for organizing a call with Gradle to get us started in the right direction.

Thanks to all [our contributors](https://github.com/clojurephant/clojurephant/graphs/contributors).
