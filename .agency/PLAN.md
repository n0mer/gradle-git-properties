# PLAN: Configurable Commit ID Abbreviation Length (Issue #234)

## Overview

Implement `commitIdAbbrevLength` configuration option following the existing `dateFormat`/`dateFormatTimeZone` pattern. Config flows: Extension -> Task -> GitProperties -> CommitIdAbbrevProperty -> GitFacade.

## TDD Approach

Each task follows RED-GREEN-REFACTOR:
1. **RED:** Write failing test first
2. **GREEN:** Implement minimal code to pass
3. **REFACTOR:** Clean up if needed

Tests are written BEFORE implementation code.

## Tasks

### Task 1: Add getAbbreviatedId(int length) to GitFacade
**Files:** 
- TEST: `src/test/groovy/com/gorylenko/jgit/GitFacadeTest.groovy`
- IMPL: `src/main/groovy/com/gorylenko/jgit/GitFacade.groovy`

**Step 1 - RED:** Write failing tests first:
```groovy
// Add to GitFacadeTest.groovy
@Test
void testGetAbbreviatedIdWithLength7() {
    // ... setup repo with commit
    assertEquals(7, facade.getAbbreviatedId(7).length())
}

@Test
void testGetAbbreviatedIdWithLength10() {
    assertEquals(10, facade.getAbbreviatedId(10).length())
}

@Test
void testGetAbbreviatedIdOnEmptyRepo() {
    assertNull(emptyFacade.getAbbreviatedId(7))
}
```

**Step 2 - GREEN:** Implement method:
```groovy
String getAbbreviatedId(int length) {
    def headId = resolveHead()
    if (headId == null) return null
    
    def objectReader = repository.newObjectReader()
    try {
        return objectReader.abbreviate(headId, length).name()
    } finally {
        objectReader.close()
    }
}
```

**Size:** S

**Depends on:** none

---

### Task 2: Modify CommitIdAbbrevProperty to accept length
**Files:**
- TEST: `src/test/groovy/com/gorylenko/properties/CommitIdAbbrevPropertyTest.groovy`
- IMPL: `src/main/groovy/com/gorylenko/properties/CommitIdAbbrevProperty.groovy`

**Step 1 - RED:** Write failing tests first:
```groovy
@Test
void testDoCallWithLength10() {
    def property = new CommitIdAbbrevProperty(10)
    def result = property.doCall(facade)
    assertEquals(10, result.length())
}

@Test
void testDoCallWithLength2() {
    def property = new CommitIdAbbrevProperty(2)
    assertEquals(2, property.doCall(facade).length())
}
```

**Step 2 - GREEN:** Implement constructor with length param:
```groovy
class CommitIdAbbrevProperty extends AbstractGitProperty {
    private final int abbrevLength

    CommitIdAbbrevProperty() {
        this(7)  // default for backward compatibility
    }

    CommitIdAbbrevProperty(int abbrevLength) {
        this.abbrevLength = abbrevLength
    }

    String doCall(GitFacade facade) {
        return isEmpty(facade) ? '' : facade.getAbbreviatedId(abbrevLength)
    }
}
```

**Size:** S

**Depends on:** Task 1

---

### Task 3: Add commitIdAbbrevLength to GitPropertiesPluginExtension
**Files:**
- TEST: `src/test/groovy/com/gorylenko/GitPropertiesPluginExtensionTest.groovy`
- IMPL: `src/main/groovy/com/gorylenko/GitPropertiesPlugin.groovy`

**Step 1 - RED:** Write failing tests first:
```groovy
@Test
void testDefaultCommitIdAbbrevLength() {
    assertEquals(7, extension.commitIdAbbrevLength)
}

@Test
void testSetCommitIdAbbrevLengthValid() {
    extension.commitIdAbbrevLength = 10
    assertEquals(10, extension.commitIdAbbrevLength)
}

@Test(expected = IllegalArgumentException)
void testSetCommitIdAbbrevLengthTooSmall() {
    extension.commitIdAbbrevLength = 1
}

@Test(expected = IllegalArgumentException)
void testSetCommitIdAbbrevLengthTooLarge() {
    extension.commitIdAbbrevLength = 41
}
```

**Step 2 - GREEN:** Implement field and setter:
```groovy
int commitIdAbbrevLength = 7

void setCommitIdAbbrevLength(int length) {
    if (length < 2 || length > 40) {
        throw new IllegalArgumentException(
            "commitIdAbbrevLength must be between 2 and 40, got: ${length}")
    }
    this.commitIdAbbrevLength = length
}
```

**Size:** S

**Depends on:** none

---

### Task 4: Update GitProperties.generate() signature
**Files:**
- TEST: `src/test/groovy/com/gorylenko/GitPropertiesTest.groovy` (create if needed)
- IMPL: `src/main/groovy/com/gorylenko/GitProperties.groovy`

**Step 1 - RED:** Write failing tests first:
```groovy
@Test
void testGenerateWithCustomAbbrevLength10() {
    def props = gitProperties.generate(gitDir, ['git.commit.id.abbrev'], 
        null, null, null, null, [:], 10)
    assertEquals(10, props['git.commit.id.abbrev'].length())
}

@Test
void testGenerateWithDefaultAbbrevLength7() {
    def props = gitProperties.generate(gitDir, ['git.commit.id.abbrev'], 
        null, null, null, null, [:], 7)
    assertEquals(7, props['git.commit.id.abbrev'].length())
}
```

**Step 2 - GREEN:** Update signatures and wiring:
1. Update `generate()` signature (line 55-56):
```groovy
public Map<String, String> generate(File dotGitDirectory, List<String> keys, 
    String dateFormat, String dateFormatTimeZone, String branch,
    Object buildVersion, Map<String, Object> customProperties, 
    int commitIdAbbrevLength) {
```

2. Update call to `getStandardPropertiesMap()` (line 60):
```groovy
Map properties = getStandardPropertiesMap(dateFormat, dateFormatTimeZone, branch, buildVersion, commitIdAbbrevLength).subMap(keys)
```

3. Update `getStandardPropertiesMap()` signature (line 82):
```groovy
private static Map getStandardPropertiesMap(String dateFormat, String dateFormatTimeZone, 
    String branch, Object buildVersion, int commitIdAbbrevLength) {
```

4. Update `CommitIdAbbrevProperty` instantiation (line 87):
```groovy
, (KEY_GIT_COMMIT_ID_ABBREVIATED)    : new CommitIdAbbrevProperty(commitIdAbbrevLength)
```

5. Update `getStandardProperties()` to pass default (line 78-79):
```groovy
public static List getStandardProperties() {
    return getStandardPropertiesMap(null, null, null, null, 7).keySet() as List
}
```

**Size:** M

**Depends on:** Task 2

---

### Task 5: Update GenerateGitPropertiesTask to pass config
**Files:** `src/main/groovy/com/gorylenko/GenerateGitPropertiesTask.groovy`

**Changes:**
Update the `generate()` call in `generateProperties()` (lines 99-101):
```groovy
Map<String, String> newMap = builder.generate(dotGitDirectory,
        gitProperties.keys, gitProperties.dateFormat, gitProperties.dateFormatTimeZone, 
        gitProperties.branch, projectVersion.get(), gitProperties.customProperties,
        gitProperties.commitIdAbbrevLength)
```

**Test:** Functional test verifies this works end-to-end

**Size:** S

**Depends on:** Task 3, Task 4

---

### Task 6: Add functional test for commitIdAbbrevLength
**Files:** `src/test/groovy/com/gorylenko/BasicFunctionalTest.groovy`

**Changes:**
Add new test method:
```groovy
@Test
public void testCustomCommitIdAbbrevLength() {
    def projectDir = temporaryFolder.newFolder()

    GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
        gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
    })

    new File(projectDir, "settings.gradle") << ""
    new File(projectDir, "build.gradle") << """
        plugins {
            id('com.gorylenko.gradle-git-properties')
        }
        gitProperties {
            commitIdAbbrevLength = 10
        }
    """.stripIndent()

    def runner = GradleRunner.create()
            .withPluginClasspath()
            .withArguments("generateGitProperties")
            .withProjectDir(projectDir)

    def result = runner.build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
    
    // Verify the abbreviated commit ID length
    def propsFile = new File(projectDir, "build/resources/main/git.properties")
    def props = new Properties()
    propsFile.withInputStream { props.load(it) }
    def abbrevId = props.getProperty("git.commit.id.abbrev")
    assertEquals(10, abbrevId.length())
    assertTrue(abbrevId.matches('[a-f0-9]{10}'))
}

@Test
public void testInvalidCommitIdAbbrevLengthFailsFast() {
    def projectDir = temporaryFolder.newFolder()

    GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
        gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
    })

    new File(projectDir, "settings.gradle") << ""
    new File(projectDir, "build.gradle") << """
        plugins {
            id('com.gorylenko.gradle-git-properties')
        }
        gitProperties {
            commitIdAbbrevLength = 1
        }
    """.stripIndent()

    def runner = GradleRunner.create()
            .withPluginClasspath()
            .withArguments("generateGitProperties")
            .withProjectDir(projectDir)

    def result = runner.buildAndFail()
    assertThat(result.output, containsString("commitIdAbbrevLength must be between 2 and 40"))
}
```

**Test:** This IS the test

**Size:** M

**Depends on:** Task 5

---

### Task 7: Update README documentation
**Files:** `README.md`

**Changes:**
1. Update property table (line ~95) to clarify configurable length:
```markdown
| `git.commit.id.abbrev` | Abbreviated commit SHA (default 7 characters, configurable) |
```

2. Add new section after "Date Format" (line ~85):
```markdown
### Commit ID Abbreviation Length

Configure the length of `git.commit.id.abbrev` (default: 7, range: 2-40):

\```groovy
gitProperties {
    commitIdAbbrevLength = 10
}
\```
```

**Test:** Visual review

**Size:** S

**Depends on:** Task 5

---

### Task 8: Verify backward compatibility
**Files:** No changes, verification only

**Changes:** None - run existing tests to ensure:
1. Default behavior (7 chars) unchanged
2. Custom properties using `it.head().abbreviatedId` still work (returns 7 chars from GitCommit)
3. All existing functional tests pass

**Test:** Run full test suite: `./gradlew test`

**Size:** S

**Depends on:** Task 6, Task 7

---

## Execution Order

```
Task 1 (GitFacade)          Task 3 (Extension)
       \                          /
        \                        /
         v                      v
        Task 2 (Property) -----> Task 4 (GitProperties)
                                       |
                                       v
                                 Task 5 (Task)
                                       |
                                       v
                                 Task 6 (Functional Test)
                                       |
                                       v
                                 Task 7 (Verification)
```

**Parallel opportunities:**
- Task 1 and Task 3 can be done in parallel
- All tasks have small scope, making parallelism less critical

## Summary

| Task | Description | Size | Depends On |
|------|-------------|------|------------|
| 1 | GitFacade.getAbbreviatedId(int) | S | - |
| 2 | CommitIdAbbrevProperty(int) | S | 1 |
| 3 | Extension.commitIdAbbrevLength | S | - |
| 4 | GitProperties.generate() signature | M | 2 |
| 5 | GenerateGitPropertiesTask pass config | S | 3, 4 |
| 6 | Functional tests | M | 5 |
| 7 | README documentation | S | 5 |
| 8 | Backward compat verification | S | 6, 7 |

**Total estimated effort:** ~4 hours
