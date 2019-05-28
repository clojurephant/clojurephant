<!-- Why is this change being made? -->

**Related issues:**

## Contributor Checklist

- [ ] Review [Contributing Guidelines](https://github.com/gradle-clojure/gradle-clojure/blob/master/.github/CONTRIBUTING.md).
- [ ] Commits contain discrete changes, messages include context about why the change was made and reference the relevant issues.
- [ ] Provide functional tests. (under `modules/gradle-clojure-plugin/src/compatTest`)
- [ ] Update documentation for user-facing changes. (under `docs/`)
- [ ] Ensure all verification tasks pass locally. (`./gradlew check`)
- [ ] Ensure CI builds pass on all Java versions. (watch the checks tab once the PR is opened)

  **TIP:** If troubleshooting a CI failure, look for the build scan URL near the bottom of the Gradle output.
