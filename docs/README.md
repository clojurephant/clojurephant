# gradle-clojure

{% include nav.md %}

## Quick Start

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

Create a new ClojureScript appliation:

```
clj -A:new gradle-cljs-app myname/myapp
```

If the documentation doesn't answer your questions, please visit either the [ClojureVerse gradle-clojure channel](https://clojureverse.org/c/projects/gradle-clojure) or the [Clojurian's Slack #gradle channel](http://clojurians.net/).

## Plugins

gradle-clojure uses the common pattern of providing _capability_ plugins and _convention_ plugins. Capability plugins provide the basic machinery for using the language, but leaves it to you to configure. Convention plugins provide configuration on top of the capabilities to support common use cases.

| Convention                     | Capability                          |
| ------------------------------ | ----------------------------------- |
| `gradle-clojure.clojure`       | `gradle-clojure.clojure-base`       |
| `gradle-clojure.clojurescript` | `gradle-clojure.clojurescript-base` |

### gradle-clojure.clojure-base

- Applies `java-base`, which lets you configure source sets. Each source set will get:
  - A Java compilation task
  - Configurations for compile (`implementation`, `compileOnly`) and runtime (`runtimeOnly`) dependencies
- Applies the internal `ClojureCommonBasePlugin` which:
  - Includes the gradle-clojure-tools JAR on the compile and runtime classpaths (for use by any Clojure tasks)
- Adds a `clojure` extension which allows you to configure builds of your Clojure code.
  - A build is added for each source set (with the same name as that source set)
  - Additional builds can be configured by the user
  - Each build gets a `check<Build>Clojure` task that can be used to ensure namespaces compile, and optionally warn or fail on reflection. (by default no namespaces are compiled)
  - Each build gets a `compile<Build>Clojure` task that can be used for AOT compilation. (by default no namespaces are AOTd)
  - If any namespaces are configured to be AOTed for the source sets build, the source sets output will be the AOTd classes. Otherwise, the Clojure source will be the output (i.e. what would get included in a JAR)

#### Clojure Builds

```groovy
clojure {
 builds {
   // Defaults noted here are for custom builds, the convention plugin configures the builds it adds differently
   mybuild {
     sourceSet = sourceSets.mystuff // no default
     // Configuration of the check<Build>Clojure task
     reflection = 'fail' // defaults to 'silent', can also be 'warn'
     checkNamespaces = ['my.core', 'my.base'] // defaults to no namespaces checked
     checkNamespaces.add('my-core') // just add a single namespace
     checkAll() // checks any namespaces found in the source set
     // Configuration of the compile<Build>Clojure task
     compiler {
       disableLocalsClearing = true // defaults to false
       elideMeta = ['doc', 'file'] // defaults to empty list
       directLinking = true // defaults to false
     }
     aotNamespaces = ['my.core', 'my.base'] // defaults to no namespaces aoted
     aotNamespaces.add('my-core') // just add a single namespace
     aotAll() // aots any namespaces found in the source set
   }
 }
}
```

### gradle-clojure.clojure

- Applies the `gradle-clojure.clojure-base` plugin (see above)
- Applies the `java` plugin:
  - Creates a main source set, whose output is packaged into a JAR via the `jar` task.
  - Creates a test source set, which extends the main source set.
  - Creates a `test` task that runs tests within the test source set.
- Applies the internal `ClojureCommonPlugin` which:
  - Creates a dev source set, to be used for REPL development, which extends the test source set.
  - Adds 'nrepl:nrepl:0.5.1' as a dependency of that source set.
  - Adds a `clojureRepl` task which will start an nREPL server.
  - Configures dependency rules to indicate that:
    - `org.clojure:tools.nrepl` is replaced by `nrepl:nrepl`
    - If you are using a Java 9+ JVM, any `org.clojure:java.classpath` dependency must be bumped to at least 0.3.0 to support the new classloader hierarchy.
- Configures the `main` Clojure build to `checkAll()` namespaces.
- Configures any build whose name includes `test` to:
  - `aotAll()` namespaces (required for the current JUnit4 integration)
  - Compile the other namespaces from the tools JAR needed by the JUnit4 integration
- Configures the `dev` Clojure build to `checkNamespaces = ['user']` (if you have a user namespace). This ensures that your REPL will start successfully.

### gradle-clojure.clojurescript-base

- Applies `java-base`, which lets you configure source sets. Each source set will get:
  - A Java compilation task
  - Configurations for compile (`implementation`, `compileOnly`) and runtime (`runtimeOnly`) dependencies
- Applies the internal `ClojureCommonBasePlugin` which:
  - Includes the gradle-clojure-tools JAR on the compile and runtime classpaths (for use by any Clojure tasks)
- Adds a `clojurescript` extension which allows you to configure builds of your ClojureScript code.
  - A build is added for each source set (with the same name as that source set)
  - Additional builds can be configured by the user
  - Each build gets a `compile<Build>ClojureScript` task that can be used for compilation. (by default no compiler options are set)
  - If `outputTo` is configured (either the top level one or for a module) for the source sets build, the source sets output will be the compiled JS. Otherwise, the ClojureScript source will be the output (i.e. what would get included in a JAR).

#### ClojureScript Builds

**NOTE:** While Figwheel options are available when `clojurescript-base` is applied, the necessary dependencies and middleware are not configured unless you apply `clojurescript`.

See [ClojureScript compiler options](https://clojurescript.org/reference/compiler-options) and [Figwheel Main configuration options](https://github.com/bhauman/lein-figwheel/blob/master/figwheel-main/doc/figwheel-main-options.md) for details on what each option does and defaults to.

```groovy
clojurescript {
 builds {
   // Defaults noted here are for custom builds, the convention plugin configures the builds it adds differently
   mybuild {
     sourceSet = sourceSets.mystuff // no default
     // Configuration of the compile<Build>ClojureScript task (defaults match what is defaulted in the ClojureScript compile options)
     compiler {
       outputTo = 'public/some/file/path.js' // path is relative to the task's destinationDir
       outputDir = 'public/some/path' // path is relative to the task's destinationDir
       optimizations = 'advanced'
       main = 'foo.bar'
       assetPath = 'public/some/path'
       sourceMap = 'public/some/file/path.js.map' // path is relative to the task's destinationDir
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
     figwheel {
       watchDirs.from = files() // defaults to the source set's CLJS source dirs
       cssDirs.from = files('src/main/resources/public/css') // defaults to empty
       ringHandler = 'my-project.server/handler'
       ringServerOptions = [port: 1234, host: 'my.domain.com']
       rebelReadline = false
       pprintConfig = true
       openFileCommand = 'myfile-opener'
       figwheelCore = false
       hotReloadCljs = false
       connectUrl = 'ws://[[config-hostname]]:[[server-port]]/figwheel-connect'
       openUrl = 'http://[[server-hostname]]:[[server-port]]'
       reloadCljFiles = false
       logFile = file('figwheel-main.log')
       logLevel = 'error'
       clientLogLevel = 'warning'
       logSyntaxErrorStyle = 'concise'
       loadWarningedCode = true
       ansiColorOutput = false
       validateConfig = false
       launchNode = false
       inspectNode = false
       nodeCommand = 'node'
       cljsDevtools = false
     }
   }
 }
}
```

### gradle-clojure.clojurescript

- Applies the `gradle-clojure.clojurescript-base` plugin (see above)
- Applies the `java` plugin:
  - Creates a main source set, whose output is packaged into a JAR via the `jar` task.
  - Creates a test source set, which extends the main source set.
  - Creates a `test` task that runs tests within the test source set.
- Applies the internal `ClojureCommonPlugin` which:
  - Creates a dev source set, to be used for REPL development, which extends the test source set.
  - Adds 'nrepl:nrepl:0.5.1' as a dependency of that source set.
  - Adds a `clojureRepl` task which will start an nREPL server.
  - Configures dependency rules to indicate that:
    - `org.clojure:tools.nrepl` is replaced by `nrepl:nrepl`
    - If you are using a Java 9+ JVM, any `org.clojure:java.classpath` dependency must be bumped to at least 0.3.0 to support the new classloader hierarchy.
- Wires your ClojureScript build configuration into the nREPL for use by Figwheel.
- Configures the REPL for Piggieback:
  - Adds a dev dependency `cider:piggieback:0.3.10`
  - Adds the Piggieback middleware: `cider.piggieback/wrap-cljs-repl`
- Configures the REPL for Figwheel:
  - Adds a dev dependency `com.bhauman:figwheel-main:0.1.2`

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

### ClojureNRepl

```groovy
clojureRepl {
  port = 55555 // defaults to a random open port (which will be printed in the build output)

  // handler and middleware are both optional, but don't provide both
  handler = 'cider.nrepl/cider-nrepl-handler' // fully-qualified name of function
  middleware = ['my.stuff/wrap-stuff'] // list of fully-qualified middleware function names (override any existing)
  middleware 'dev/my-middleware', 'dev/my-other-middleware' // one or more full-qualified middleware function names (append to any existing)

  // clojureRepl provides fork options to customize the Java process for compilation
  forkOptions {
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

### check or compile tasks

Always configure compiler options and reflection settings via the `clojure` or `clojurescript` extensions. These options may be immutable on the tasks at some point in the future.

The only settings you should configure directly on the tasks are the forkOptions, if you need to customize the JVM that is used.

```groovy
checkClojure {
  // to customize the Java process for compilation
  forkOptions {
    memoryMaximumSize = '2048m'
    jvmArgs = ['-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005', '-Djava.awt.headless=true']
  }
}
```
