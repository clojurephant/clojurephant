# gradle-clojure

{% include nav.md %}

## gradle-clojure.clojure-base

It's unlikely you will apply this plugin directly. However, if you do, here's what you'll get:

- The `java-base` plugin will be applied (which provides basic Java compilation support, but no source sets).
- Any source sets you add will have a Clojure source directory added with a corresponding compile and check task.

## gradle-clojure.clojure

When applied this plugin:

- Applies `gradle-clojure.clojure-base`
- Applies `java` (i.e. you'll get Java compilation support and `main` and `test` source sets).
- Configures the `test` task to include AOT compiled test classes in order to generate stub classes with JUnit runners.
- Adds a `dev` source set for use by the REPL. Its classpath includes the `main` and `test` classpaths.
- Adds a `clojureRepl` to start an nREPL server using the `dev` classpath.
- Adds an `aotJar` task to package a JAR with AOT compiled classes instead of Clojure source.

## gradle-clojure.clojurescript-base

- The `java-base` plugin will be applied (which provides basic Java compilation support, but no source sets)
- Any source sets you add will have a ClojureScript source directory added with a corresponding compile task.

## Project Layout

```
<project>/
  src/
    main/
      clojure/
        sample_clojure/
          core.clj
      clojurescript/
        sample_clojure/
          main.cljs
    test/
      clojure/
        sample_clojure/
          core_test.clj
      clojurescript/
        sample_clojure/
          main_test.cljs // right now we don't support cljs.test
    dev/
      clojure/
        user.clj
      clojurescript/
        user.cljs
  gradle/
    wrapper/
      gradle-wrapper.jar
      gradle-wrapper.properties
  build.gradle
  gradlew
  gradlew.bat
```

## Task Configuration

### ClojureCheck

The check task will load all of your sources to make sure they compile.

```groovy
checkClojure {
  options {
    reflectionWarnings {
      enabled = true             // Defaults to false
      projectOnly = true         // Only show warnings from your project, not dependencies - default false
      asErrors = true            // Treat reflection warnings as errors and fail the build
                                 // If projectOnly is true, only warnings from your project are errors.
    }

    // compileClojure provides fork options to customize the Java process for compilation
    forkOptions {
      memoryMaximumSize = '2048m'
      jvmArgs = ['-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005', '-Djava.awt.headless=true']
    }
  }
}
```

### ClojureCompile

The compile task will always compile your sources.

```groovy
compileClojure {
  options {
    // Compiler options for AOT
    disableLocalsClearing = true                 // Defaults to false
    elideMeta = ['doc', 'file', 'line', 'added'] // Defaults to []
    directLinking = true                         // Defaults to false

    // compileClojure provides fork options to customize the Java process for compilation
    forkOptions {
      memoryMaximumSize = '2048m'
      jvmArgs = ['-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005', '-Djava.awt.headless=true']
    }
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

  // handler and middleware are both optional, but don't provide both
  handler = 'cider.nrepl/cider-nrepl-handler' // fully-qualified name of function
  middleware = ['my.stuff/wrap-stuff'] // list of fully-qualified middleware function names (override any existing)
  middleware 'dev/my-middleware', 'dev/my-other-middleware' // one or more full-qualified middleware function names (append to any existing)

  // clojureRepl provides fork options to customize the Java process for compilation
  options.forkOptions {
    memoryMaximumSize = '2048m'
    jvmArgs = ['-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005', '-Djava.awt.headless=true']
  }
}
```

The `ClojureNRepl` task also supports command-line options for some of it's parameters. Multiple `middleware` must be specified as separate options.

```
./gradlew clojureRepl --port=1234 --handler=cider.nrepl/cider-nrepl-handler
./gradlew clojureRepl --port=4321 --middleware=dev/my-middleware --middleware=dev/my-other-middleware
```

### ClojureScriptCompile

Review [ClojureScript's compiler options](https://clojurescript.org/reference/compiler-options) documentation for the meaning of all of these features. Differences from the standard behavior are documented inline in the example below.

```groovy
compileClojurescript {
  options {
    outputTo = 'some/file/path.js' // path is relative to the task's destinationDir
    outputDir = 'some/path' // path is relative to the task's destinationDir
    optimizations = 'advanced'
    main = 'foo.bar'
    assetPath = 'js/compiled/out'
    sourceMap = 'some/file/path.js.map' // path is relative to the task's destinationDir
    verbose = true
    prettyPrint = false
    target = 'nodejs'
    // foreignLibs
    externs = ['jquery-externs.js']
    // modules
    // stableNames
    preloads = ['foo.dev']
    npmDeps = ['lodash': '4.17.4']
    installDeps = true
    checkedArrays = 'warn'
  }
}
```
