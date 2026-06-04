# Reviewer Gate 2: Functional Tests for gitPropertiesName JAR Packaging

## Test File Location
`src/test/groovy/com/gorylenko/BasicFunctionalTest.groovy`

## 5 New Tests Review

### 1. testGitPropertiesNameSubpathInJar (lines 435–488)
- Uses `java` plugin + `assemble` task
- Sets `gitPropertiesName = 'discord4j/common/git.properties'`
- Asserts file on disk at `build/generated/resources/git/discord4j/common/git.properties`
- Asserts JAR entry `discord4j/common/git.properties` is NOT null
- Asserts JAR entry `git.properties` (root) IS null
- ZipFile closed in `finally` block: YES
- **Status: CORRECT**

### 2. testGitPropertiesNamePlainFilenameInJar (lines 496–543)
- Uses `java` plugin + `assemble` task
- Sets `gitPropertiesName = 'git-info.properties'`
- Asserts file on disk at `build/generated/resources/git/git-info.properties` (no subdir)
- Asserts JAR entry `git-info.properties` at root is NOT null
- ZipFile closed in `finally` block: YES
- **Status: CORRECT**

### 3. testDefaultGitPropertiesNameInJar (lines 549–590)
- Uses `java` plugin + `assemble` task
- No `gitPropertiesName` set (pure default regression guard)
- Asserts JAR entry `git.properties` at root is NOT null
- ZipFile closed in `finally` block: YES
- **Status: CORRECT**

### 4. testGitPropertiesNameLeadingSlashFailsFast (lines 596–620)
- Sets `gitPropertiesName = '/git.properties'`
- Calls `buildAndFail()`
- Asserts output contains "must be a relative path that stays under gitPropertiesResourceDir"
- **Status: CORRECT**

### 5. testGitPropertiesNameDotDotSegmentFailsFast (lines 626–650)
- Sets `gitPropertiesName = '../git.properties'`
- Calls `buildAndFail()`
- Asserts output contains "must be a relative path that stays under gitPropertiesResourceDir"
- **Status: CORRECT**

## ZipFile Resource Management
All three JAR-inspection tests (`testGitPropertiesNameSubpathInJar`, `testGitPropertiesNamePlainFilenameInJar`, `testDefaultGitPropertiesNameInJar`) properly close `ZipFile` in `finally` blocks. No resource leaks.

## Test Run Results

Full test suite executed with `./gradlew test --rerun-tasks`.

All 5 new tests passed:
- `BasicFunctionalTest > testGitPropertiesNameSubpathInJar PASSED`
- `BasicFunctionalTest > testGitPropertiesNamePlainFilenameInJar PASSED`
- `BasicFunctionalTest > testDefaultGitPropertiesNameInJar PASSED`
- `BasicFunctionalTest > testGitPropertiesNameLeadingSlashFailsFast PASSED`
- `BasicFunctionalTest > testGitPropertiesNameDotDotSegmentFailsFast PASSED`

No failures or errors anywhere in the full test suite (all tests PASSED, BUILD SUCCESSFUL).

SIGNAL: PASS
