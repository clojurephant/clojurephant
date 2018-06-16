# gradle-clojure

[![Bintray](https://api.bintray.com/packages/gradle-clojure/maven/gradle-clojure/images/download.svg)](https://bintray.com/gradle-clojure/maven/gradle-clojure/_latestVersion)
[![CircleCI](https://circleci.com/gh/gradle-clojure/gradle-clojure.svg?style=svg)](https://circleci.com/gh/gradle-clojure/gradle-clojure)

## What is this?

A Gradle plugin providing support for the Clojure and ClojureScript languages.

**NOTE:** gradle-clojure should not be considered stable until 1.0.0. Until then, minor versions (e.g. 0.1.0 to 0.2.0) will likely contain breaking changes.

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
- Figwheel (via [figwheel.main](https://github.com/bhauman/lein-figwheel/tree/master/figwheel-main))

## Why should you care?

The goal is to provide the same creature comforts that [Leiningen](http://leiningen.org/) and [Boot](http://boot-clj.com/) do for Clojure/ClojureScript development, while also leveraging Gradle's unique features:

- Strong support for polyglot projects
- Strong support for multi-project builds
- Large [plugin ecosystem](https://plugins.gradle.org)

## Usage

See the [Release Notes](https://github.com/gradle-clojure/gradle-clojure/releases) for available versions, compatibility with Gradle, Java, and Clojure, and detailed change notes.

This plugin assumes you're using a sane layout for your Clojure code - namespaces corresponding
to your source code layout, and one namespace per file. The plugin uses the filenames to
calculate the namespaces involved, it does not parse the files looking for `ns` forms.

### Quick Start

- Install the [Clojure command line tool](https://clojure.org/guides/getting_started) (i.e. clj).
- Add an alias for [clj-new](https://github.com/seancorfield/clj-new/) to your `~/.clojure/deps.edn`

Create a new Clojure library:

```
clj -A:new gradle-clj-lib myname/mylib
```

Create a new Clojure application:

```
clj -A:new gradle-clj-app myname/myapp
```

ClojureScript templates coming soon.

#### Common Commands

- `./gradlew test` Executes your clojure.test tests (and any other JUnit tests in your build).
- `./gradlew clojureRepl` Starts an nREPL server (on a random port by default).

**build.gradle**

```groovy
plugins {
  id "gradle-clojure.clojure" version "<version>"
}

dependencies {
  // whatever version of clojure you prefer (versions before 1.8.0 may not be compatible)
  implementation 'org.clojure:clojure:1.9.0'
  // and any other dependencies you want on the compile classpath
  // implementation 'group:artifact:version'

  // needed for test integration
  testImplementation 'junit:junit:4.12'
  // and any other test-specific dependencies
  // testImplementation 'group:artifact:version'

  // dependencies for REPL use only
  devImplementation 'org.clojure:tools.namespace:0.3.0-alpha4'
}
```

See all available options in the [docs](https://gradle-clojure.github.io).

## Getting help

Please use the repo's [issues](https://github.com/gradle-clojure/gradle-clojure/issues) for all questions, bug reports, and feature requests.

## Contributing

See the [guidelines](.github/CONTRIBUTING.md) for details on how you can contribute.

## Acknowledgements

This project started from the [cursive.clojure](https://github.com/cursive-ide/gradle-clojure) plugin by Colin Fleming (@cmf, original author) and Piotrek Bzdyl (@pbzdyl).

Thanks to John Szakmeister (@jszakmeister) for organizing a call with Gradle to get us started in the right direction.

Thanks to all [our contributors](https://github.com/gradle-clojure/gradle-clojure/graphs/contributors).
