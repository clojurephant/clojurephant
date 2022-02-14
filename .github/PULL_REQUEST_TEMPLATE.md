<!-- Why is this change being made? -->

**Related issues:**

## Contributor Checklist

- [ ] Review [Contributing Guidelines](https://github.com/clojurephant/clojurephant/blob/master/.github/CONTRIBUTING.md).
- [ ] Commits contain discrete changes, messages include context about why the change was made and reference the relevant issues.
- [ ] Commit messages should be prefixed with one of the following (these are used to determine the next version we release):
  - `patch: ` if the change added no new functionality and is backwards compatible
  - `minor: ` if the change added new functionality and is backwards compatible
  - `major: ` if the change is not backwards compatible
  - `chore: ` if the change doesn't affect plugin logic/behavior at all (and obviously still backwards compatible)
  - Any of these can optionally specify an area of the plugin they affected in parentheses (e.g. `patch(clojurescript): `)
    - `build`
    - `docs`
    - `clojure`
    - `clojurescript`
    - `common`
    - `repl`
- [ ] Provide functional tests. (under `clojurephant-plugin/src/compatTest`)
- [ ] Update documentation for user-facing changes. (under `docs/`)
- [ ] Ensure all verification tasks pass locally. (`./gradlew check`)
- [ ] Ensure CI builds pass on all Java versions. (watch the checks tab once the PR is opened)

**TIP:** If troubleshooting a CI failure, look for the build scan URL in the workflow run summary.
