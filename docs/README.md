# gradle-clojure

## gradle-clojure.clojure-base

It's unlikely you will apply this plugin directly. However, if you do, here's what you'll get:

- The `java-base` plugin will be applied (which provides basic Java compilation support, but no source sets).
- Any source sets you add will have a Clojure source directory added with a corresponding compile task.

## gradle-clojure.clojure

When applied this plugin:

- Applies `gradle-clojure.clojure-base`
- Applies `java` (i.e. you'll get Java compilation support and `main` and `test` source sets).
- Configures the `compileTestClojure` task to AOT compile in order to generate stub classes with JUnit runners.
- Adds a `dev` source set for use by the REPL. Its classpath includes the `main` and `test` classpaths.
- Adds a `clojureRepl` to start an nREPL server using the `dev` classpath.

## Project Layout

```
<project>/
  src/
    main/
      clojure/
        sample_clojure/
          core.clj
    test/
      clojure/
        sample_clojure/
          core_test.clj
    dev/
      clojure/
        user.clj
  gradle/
    wrapper/
      gradle-wrapper.jar
      gradle-wrapper.properties
  build.gradle
  gradlew
  gradlew.bat
```

## Task Configuration

### ClojureCompile

```groovy
compileClojure {
  options.aotCompile = true            // Defaults to false
  options.copySourceSetToOutput = false   // Defaults to !aotCompile

  options.reflectionWarnings {
    enabled = true             // Defaults to false
    projectOnly = true         // Only show warnings from your project, not dependencies - default false
    asErrors = true            // Treat reflection warnings as errors and fail the build
                               // If projectOnly is true, only warnings from your project are errors.
  }

  // Compiler options for AOT
  options.disableLocalsClearing = true                 // Defaults to false
  options.elideMeta = ['doc', 'file', 'line', 'added'] // Defaults to []
  options.directLinking = true                         // Defaults to false

  // compileClojure provides fork options to customize the Java process for compilation
  options.forkOptions {
    memoryMaximumSize = '2048m'
    jvmArgs = ['-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005', '-Djava.awt.headless=true']
  }
}
```

### Test

```groovy
test {
  // this is a standard Gradle Test task
  // https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html
}
```

### ClojureNRepl

```groovy
clojureRepl {
  port = 55555 // defaults to a random open port (which will be printed in the build output)

  // clojureRepl provides fork options to customize the Java process for compilation
  options.forkOptions {
    memoryMaximumSize = '2048m'
    jvmArgs = ['-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005', '-Djava.awt.headless=true']
  }
}
```

## Polyglot Projects

### Clojure that depends on Java

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

### Java that depends on Clojure

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
  preCompile.extendsFrom compile
}

// dependencies, etc...
```

**NOTE:** you could be more thorough in your configuration to get all of the
configurations to line up as you expect, but this covers the main use case.
