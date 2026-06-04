## Scenario Parity Report

### v4.0.1 test results

- **Branch:** main (version 4.0.0 jar → `gradle-git-properties-4.0.0.jar`)
- **Tests run:** 306
- **PASSED:** 306
- **FAILED:** 0
- **BUILD:** SUCCESSFUL

### v4.0.2 test results

- **Branch:** feat-git-properties-jar-path (version 4.0.2 jar → `gradle-git-properties-4.0.2.jar`)
- **Tests run:** 319
- **PASSED:** 319
- **FAILED:** 0
- **BUILD:** SUCCESSFUL

### Parity

PASS — No regressions. All 306 tests that exist in v4.0.1 also pass in v4.0.2. The 13-test delta is accounted for entirely by new tests added for the `gitPropertiesName` JAR-path feature.

### New scenarios (v4.0.2 only)

All 5 new functional test scenarios pass:

| Test | Result |
|------|--------|
| `testGitPropertiesNameSubpathInJar` — `gitPropertiesName` with subpath (e.g. `discord4j/common/git.properties`) → correct JAR entry at that subpath, no root entry | PASS |
| `testGitPropertiesNamePlainFilenameInJar` — `gitPropertiesName` plain filename (e.g. `git-info.properties`) → correct root JAR entry | PASS |
| `testDefaultGitPropertiesNameInJar` — no `gitPropertiesName` set → default `git.properties` at JAR root (regression guard) | PASS |
| `testGitPropertiesNameLeadingSlashFailsFast` — `gitPropertiesName = '/git.properties'` → build fails with validation error | PASS |
| `testGitPropertiesNameDotDotSegmentFailsFast` — `gitPropertiesName = '../git.properties'` → build fails with validation error | PASS |

### Summary

- v4.0.1: 306 tests, 306 PASSED, 0 FAILED
- v4.0.2: 319 tests, 319 PASSED, 0 FAILED
- Parity delta: +13 tests (all new, all passing)
- No regressions detected
