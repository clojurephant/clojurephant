# Contributing to gradle-clojure

## Asking a question or getting help

For now, create an issue. We'll decide on an alternate location for usage help in the future.

## Submitting a bug report or feature request

Create a new issue and fill out the template. Thanks for providing feedback on what's important to you as a user!

## Roadmap

See the [milestones](https://github.com/gradle-clojure/gradle-clojure/milestones) for details on planned features.

## Contributing documentation or code

Pull requests are very welcome. Thanks in advance for helping the project (that goes double for those of you updating documentation)!

- **For minor changes:** Go right ahead and submit a PR.
- **For already open issues:** Comment in the issue that you'd like to help out on to ensure it's not already being worked on and to get guidance from the team.
- **For new features:** Open an issue first detailing the feature. This will let the team provide feedback on how that feature can work best for the project.

### Development Setup

- Clone this repo.
- Have a Java 8 or higher JDK installed.
- If using Eclipse or Intellij:
  - Import the project as a Gradle project.
  - Import the Eclipse formatter preferences from `.gradle/eclipse-java-formatter.xml`. (See _Code Style_ for more information.)

### Project Structure

- Documentation is under `docs/`.
  - If you add a new page, make sure to put a link in the `docs/_includes/nav.md` and include the nav in your page.
- Modules:
  - Plugin itself is in `gradle-clojure-plugin/`
  - Tools library used by Gradle tasks is in `gradle-clojure-tools/`
- Test suite:
  - Functional Gradle tests (run against a range of Gradle versions) are in `gradle-clojure-plugin/src/compatTest`
- Templates:
  - clj-new templates are under `templates/*`

### Gradle Resources

A few helpful resources if you're new to writing Gradle plugins:

- [Gradle User Manual](https://docs.gradle.org/current/userguide/userguide.html), specifically:
  - [Designing Gradle plugins](https://guides.gradle.org/designing-gradle-plugins/) (see bottom of guide for links to further guides)
  - [Lazy task configuration](https://docs.gradle.org/current/userguide/lazy_configuration.html)
- [Gradle DSL Reference](https://docs.gradle.org/current/dsl/)

### Testing Templates

#### Test Existing Template

1. Run `./gradlew :templates:<template project name>:newProject` (e.g. `./gradlew :templates:gradle-clj-lib:newProject`)
1. Navigate into the created project (Projects will be added under `templates-test/sample-*`).
1. Use the project like normal to test its functionality.

#### Create New Template

1. Create a new directory under `templates/` with a name matching what the template should be called. All templates should start with `gradle-`.
1. Update the `settings.gradle` to list the new template project.
1. Create the `templates/<my-template>/src/main/clojure` and `templates/<my-template>/src/main/resources`
1. Add template namespaces and resources (see other templates as examples and clj-new's documentation/source).

### Code Style

This project uses the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). Google's provides [google-java-format](https://github.com/google/google-java-format) and an [Eclipse formatter profile](https://github.com/google/styleguide/blob/gh-pages/eclipse-java-google-style.xml) to help automate this. Both however have a weakness in how they line wrap, primarily for code that heavily uses lambdas. The style guide's text allows for more discretion in where line wrapping happens, but the automated ones can be overzealous. For this reason, we are using a modified version of the Eclipse profile that disables the automatic line wrapping.

The style is enforced using the [spotless](https://github.com/diffplug/spotless) plugin, which can also reformat your code to comply with the style with `./gradlew spotlessApply`.

You can import the Eclipse formatter settings `.gradle/eclipse-java-formatter.xml` to have your IDE respect the style.

## Maintainer/Collaborator Processes

### CI Configuration

gradle-clojure is built on [Circle CI](https://circleci.com/gh/gradle-clojure/gradle-clojure). This is configured in `.circleci/config.yml`.

There are two workflows:

- `main` - General build verification running on all branches and PRs on push:
  - Runs full `./gradlew check` test suites and style verification.
  - Runs on all supported Java versions (currently 8, 9, 10).
  - When a tag is pushed, it will also try to do a release/publish (i.e. _don't make a tag unless you're trying to release_).
- `weekly-deps` - Weekly run of `./gradlew check` against the latest dependencies and Gradle versions
  - This runs the same suite as `main`, but first updates our dependency lock file and Stutter lock file
  - These lock file changes are not committed back to GitHub, so it's mostly useful to see if our project will break if we updated our dependencies.
  - See the `Diff lock files` step output in the `update-dependencies` job for which ones were updated.

### Updating our dependencies

To update the lock with the latest versions matching any ranges we specified:

```
./gradlew lock --write-locks
```

### Supporting new Gradle versions

A weekly run in Circle CI will pick up new versions and test them for compatibility. To make support _official_, we should update our lock file.

The following task will update our lock files will the latest available versions that match the compatibility rules in our `stutter {}` block in `modules/gradle-clojure-plugin/build.gradle`.

```
./gradlew stutterWriteLocks
```

The `stutter {}` block can also be used to change the ranges we support. See [stutter's documentation](https://github.com/ajoberstar/gradle-stutter) for details.

### Release Process

We use [reckon](https://github.com/ajoberstar/reckon) to manage our versioning and tagging. There is no version number stored in the build file, reckon will determine this automatically from the project history and user input.

We have 3 release stages:

- `beta` - significant functionality that we'd like to release but the version isn't feature-complete yet
- `rc` - intended as a `final` release, but want to provide an opportunity for bug testing
- `final` - no known or significant bugs were found in the rc, and it's ready for general consumption

To generate a release:

- (For `rc` or `final`) Make sure all issues in [GitHub milestone](https://github.com/gradle-clojure/gradle-clojure/milestones).
- (For `final`) make sure we've released an `rc` already for this commit.
- Have the `master` branch checked out
- Run `./gradlew reckonTagPush -Preckon.scope=<scope> -Preckon.stage=<stage>` (e.g. `./gradlew reckonTagPush -Preckon.scope=patch -Preckon.stage=beta`)
  - This will run `check` on the project, create a version tag, and push that tag
  - The tag push will trigger Circle CI to run the `main` workflow, including the publish step if tests pass on all supported Java versions.
  - The publish will push the plugin to JCenter, Clojars, and the Gradle Plugin Portal.
- Go to the GitHub [releases](https://github.com/gradle-clojure/gradle-clojure/releases) and draft a new release. Use [the template](https://raw.githubusercontent.com/gradle-clojure/gradle-clojure/master/.github/RELEASE_TEMPLATE.md) for consistency. Ensure you check the _is a pre-release_ if this is a `beta` or `rc`.
