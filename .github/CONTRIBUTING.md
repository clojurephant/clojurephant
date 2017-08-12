# Contributing to gradle-clojure

## Submitting a bug report

_Coming soon_

## Submitting a feature request

_Coming soon_

## Development setup

- Clone this repo.
- Have a Java 8 or higher JDK installed.
- If using, Intellij or Eclipse, see the [google-java-format](https://github.com/google/google-java-format) README for instructions for installing a plugin to format your code consistently.
- Run `./gradlew check` to validate the current tests.

## Making changes

_Coming soon_

## Code Style

For consistency, uses the [spotless](https://github.com/diffplug/spotless) plugin, leveraging [google-java-format](https://github.com/google/google-java-format) to have an automated way to validate and reformat our Java code. This is meant to avoid bikeshedding on formatting.

The build will automatically validate this with the `spotlessCheck` task. If differences are found, you can reformat the code with `./gradlew spotlessApply`.

## Roadmap

See the [milestones](https://github.com/gradle-clojure/gradle-clojure/milestones) for details on upcoming features.
