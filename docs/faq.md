# Frequently Asked Questions

{% include nav.md %}

## How do I get dependencies from Clojars?

To get dependencies from anywhere you need to list it in your project's repositories block.

```groovy
// plugins, etc.

repositories {
  maven {
    name = 'Clojars'
    url = 'https://repo.clojars.org/'
  }
}

// dependencies, etc.
```

## How do I publish to Clojars?

You'll want to set your Clojars credentials in environment variables (in this example, `CLOJARS_USER` and `CLOJARS_PASSWORD`).

```groovy
plugins {
  id 'maven-publish'
}

// other stuff

publishing {
  publications {
    main(MavenPublication) {
      from components.java
    }
  }
  repositories {
    maven {
      name = 'clojars'
      url = 'https://repo.clojars.org'
      credentials {
        username = System.env['CLOJARS_USER']
        password = System.env['CLOJARS_PASSWORD']
      }
    }
  }
}
```

Then run the `publish` task.

## How do I create an uberjar?

Use the Gradle [Shadow plugin](http://imperceptiblethoughts.com/shadow/).

### Configuration

To create an executable uberjar:

```groovy
plugins {
  id 'gradle-clojure.clojure' version '<version>'
  // this tells Gradle you're generating an application with a main class
  id 'application'
  // Pulls in the shadow plugin which produces the uberjar
  id 'com.github.johnrengelman.shadow' version '5.0.0'
}

mainClassName = 'whatever_your.main.ns.class.is'

// normal repositories and deps blocks
```

Ensure your main namespace has `(:gen-class)` in the `ns` declaration:

```clojure
(ns sample.core
  (:require [clojure.string :as string]
            [java-time :as time])
  (:gen-class))

(defn -main [& args]
  (println (str (time/local-date))))

```

### Usage

- `./gradlew shadowJar` will produce the uberjar (look in `build/libs`)
- `./gradlew runShadow` will run the main class of your uberjar
- `./gradlew distShadowZip` or `./gradlew distShadowTar` will produce a distribution with OS-specific start scripts to run your uberjar. (look in `build/distributions`)

### More information

Read the [Shadow Plugin User Guide](http://imperceptiblethoughts.com/shadow/). for full details on their other features.

## How do I use CIDER?

[CIDER](https://cider.readthedocs.io/en/latest/) is a Clojure development environment for Emacs.

Right now you need to manually add the dependency and specify the handler/middleware.

Either apply to all of your projects via an init script:

**~/.gradle/init.d/cider.gradle**

```groovy
allprojects {
  plugins.withId('gradle-clojure.clojure') {
    dependencies {
      devImplementation 'cider:cider-nrepl:0.21.1'
    }

    clojureRepl {
      handler = 'cider.nrepl/cider-nrepl-handler'
    }
  }
}
```

Or add it manually to your project:

**build.gradle**

```groovy
dependencies {
  devImplementation 'cider:cider-nrepl:0.21.1'
}

clojureRepl {
  handler = 'cider.nrepl/cider-nrepl-handler'
}
```

Optionally, omit the handler config and provide it on the CLI:

```
./gradlew clojureRepl --handler=cider.nrepl/cider-nrepl-handler
```

Once your REPL starts, use `cider-connect` within Emacs to connect to the port listed in your Gradle output.

## How do I use Figwheel?

[Figwheel Main](https://github.com/bhauman/lein-figwheel/tree/master/figwheel-main) is included by default, if you apply the `gradle-clojure.clojurescript` plugin. You'll automatically get Piggieback added to your nREPL to support CLJS repls and your `clojurescript.builds` configuration will be available in the REPL to let you start Figwheel.

1. Start the REPL with `./gradlew clojureRepl` (or from your editor, if it supports Gradle).
1. Connect to the REPL from your favorite editor (see the port in the output).
1. Require the Figwheel helper ns: `(require '[gradle-clojure.tools.figwheel :as fw])`
1. Start your Figwheel build: `(fw/start "dev")`
1. Figwheel should open your browser when its ready to connect.

**NOTE:** Figwheel's docs will talk about `figwheel-main.edn` and `<build>.cljs.edn` files for configuration. These will probably work with your Gradle REPL, but they won't be known to Gradle when its other tasks run. Currently, we recommend configuring Figwheel in your `clojurescript.builds {}` options, though that can be a hassle when tuning build options during initial setup.

## How do I build Clojure code that depends on Java code?

You can compile Clojure code that depends on Java out of the box. Just put your
Java code in the same source set as the Clojure code:

```
<project>/
  src/
    main/
      java/
        sample_java/
          Sample.java
      clojure/
        sample_clojure/
          core.clj
```

## How do I build Java code that depends on Clojure code?

This requires introducing another source set for the Clojure code.

```
<project>/
  src/
    main/
      java/
        sample_java/
          Sample.java
    pre/
      clojure/
        sample_clojure/
          core.clj
```

**build.gradle**

```groovy
// plugins, etc...

sourceSets {
  pre
  main.compileClasspath += pre.output
}

configurations {
  preImplementation.extendsFrom implementation
}

// dependencies, etc...
```

**NOTE:** you could be more thorough in your configuration to get all of the
configurations to line up consistently, but this covers the main use case.

## How do I troubleshoot what the Clojure-specific tasks are doing?

If you use the Gradle standard `--info` or `--debug` flags, the Clojure-specific tasks provided by this plugin will also output more logging information (such as diagnostics about the worker processes).

If you only want the Clojure logging turned up and not Gradle's as a whole, use the Gradle property either:

**On the command line:**

```
./gradlew check -Pgradle-clojure.tools.logger.level=debug
```

**Or in a gradle.properties file**

```
gradle-clojure.tools.logger.level=debug
```
