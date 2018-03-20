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

## How do I create an uberjar?

Use the Gradle [Shadow plugin](http://imperceptiblethoughts.com/shadow/).

**NOTE** _We'll add more docs here for basic usage in the future._

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
  preCompile.extendsFrom compile
}

// dependencies, etc...
```

**NOTE:** you could be more thorough in your configuration to get all of the
configurations to line up consistently, but this covers the main use case.
