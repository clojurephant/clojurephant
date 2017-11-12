# Contributing to gradle-clojure

## Submitting a bug report

Create a new issue and fill out the template.

## Submitting a feature request

Create a new issue and delete the template, filling in the details of the feature
you'd like added.

## Contributing documentation or code

Pull requests are very welcome. Thanks in advance!

- **For minor changes:** Go right ahead and submit a PR.
- **For already open issues:** Comment in the issue that you'd like to help out on to ensure it's not already being worked on and to get guidance from the team.
- **For new features:** Open an issue first detailing the feature. This will let the team provide feedback on how that feature can work best for the project.

### Development setup

- Clone this repo.
- Have a Java 8 or higher JDK installed.
- If using, Intellij or Eclipse, see the [google-java-format](https://github.com/google/google-java-format) README for instructions for installing a plugin to format your code consistently.
- Run `./gradlew check` to validate the current tests.

### Code Style

This project uses the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). Google's provides [google-java-format](https://github.com/google/google-java-format) and an [Eclipse formatter profile](https://github.com/google/styleguide/blob/gh-pages/eclipse-java-google-style.xml) to help automate this. Both however have a weakness in how they line wrap, primarily for code that heavily uses lambdas. The style guide's text allows for more discretion in where line wrapping happens, but the automated ones can be overzealous. For this reason, we are using a modified version of the Eclipse profile that disables the automatic line wrapping.

The style is enforced using the [spotless](https://github.com/diffplug/spotless) plugin, which can also reformat your code to comply with the style with `./gradlew spotlessApply`.

You can import the Eclipse formatter settings `.gradle/eclipse-java-google-style-nowrap.xml` to have your IDE respect the style.

## Roadmap

See the [milestones](https://github.com/gradle-clojure/gradle-clojure/milestones) for details on upcoming features.
