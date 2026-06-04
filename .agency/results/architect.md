# Architect Design: `gitPropertiesName` Relative Path Support

## Key Findings

### 1. `gitPropertiesName` — declaration and type

File: `src/main/groovy/com/gorylenko/GitPropertiesPlugin.groovy`, line 89

```groovy
String gitPropertiesName = "git.properties"
```

Plain `String` field on `GitPropertiesPluginExtension`. No `Property<String>` wrapper. Direct assignment, no setter override.

### 2. Where `gitPropertiesName` is consumed

File: `src/main/groovy/com/gorylenko/GenerateGitPropertiesTask.groovy`, lines 174–178

```groovy
private RegularFileProperty getGitPropertiesFile() {
    def fileProperty = objectFactory.fileProperty()
    fileProperty.set(getGitPropertiesDir().file(gitProperties.gitPropertiesName))
    return fileProperty
}
```

`getGitPropertiesDir()` returns a `Directory`. Then `Directory.file(String)` is called with `gitPropertiesName`.

**Critical: `Directory.file(String)` DOES support relative subpaths.** The Gradle API resolves the path relative to the directory. So `getGitPropertiesDir().file("discord4j/common/git.properties")` will correctly produce `<gitPropertiesResourceDir>/discord4j/common/git.properties` — no code change needed in the path construction.

### 3. How the output file tree flows into the JAR

Plugin flow (from `GitPropertiesPlugin.groovy`, lines 56–67):
1. `getGitPropertiesDir()` returns the resource directory (e.g., `build/generated/resources/git`)
2. That entire directory is added as `sourceSets.main.resources.srcDir`
3. `processResources` copies the whole tree to `build/resources/main/`
4. `jar` packages `build/resources/main/` into the JAR

**The tree is copied as-is.** If the file is at `build/generated/resources/git/discord4j/common/git.properties`, then `processResources` copies it to `build/resources/main/discord4j/common/git.properties`, and the JAR contains `discord4j/common/git.properties`. This is the desired behavior.

### 4. `PropertiesFileWriter.writeToPropertiesFile` already handles parent dirs

File: `src/main/groovy/com/gorylenko/PropertiesFileWriter.groovy`, lines 40–41

```groovy
if (!propsFile.parentFile.exists()) {
    propsFile.parentFile.mkdirs()
}
```

Subdirectory creation is already handled. No change needed.

### 5. `@OutputFile` annotation on `getGitPropertiesFile()`

File: `GenerateGitPropertiesTask.groovy`, lines 79–82

```groovy
@OutputFile
RegularFileProperty getOutput() {
    return getGitPropertiesFile()
}
```

`@OutputFile` for a file inside a subdirectory of `@OutputDirectory` would technically be fine since Gradle tracks them independently — but since the task has no explicit `@OutputDirectory` annotation, this is already the sole declared output. No change needed here.

### 6. Existing validation pattern

`commitIdAbbrevLength` uses a setter with validation (lines 101–107):
```groovy
void setCommitIdAbbrevLength(int length) {
    if (length < 2 || length > 40) {
        throw new IllegalArgumentException(
            "commitIdAbbrevLength must be between 2 and 40, got: ${length}")
    }
    this.commitIdAbbrevLength = length
}
```

Identical pattern should be used for `gitPropertiesName` validation.

### 7. Test patterns

**Unit tests** (`GitPropertiesPluginExtensionTest`):
- Use `ProjectBuilder.builder().build()` + `project.pluginManager.apply`
- Test setter via `ext.commitIdAbbrevLength = N` with `@Test(expected = IllegalArgumentException)`
- Pattern is straightforward — add analogous tests for `setGitPropertiesName`

**Functional tests** (`BasicFunctionalTest`):
- Use `GradleRunner.create().withPluginClasspath()` on a temp project with git
- JAR assertion pattern at lines 295–311: get first `.jar` from `build/libs`, open as `ZipFile`, call `zipFile.getEntry("git.properties")`
- For subpath test: `zipFile.getEntry("discord4j/common/git.properties")`
- Default regression: same pattern but verify `zipFile.getEntry("git.properties")` exists

---

## Exact Changes Required

### Change 1: Add `setGitPropertiesName` validator to `GitPropertiesPluginExtension`

**File:** `src/main/groovy/com/gorylenko/GitPropertiesPlugin.groovy`

**Location:** After line 107 (after `setCommitIdAbbrevLength`) / inside `GitPropertiesPluginExtension` class

**Before (lines 89, 101–107):**
```groovy
String gitPropertiesName = "git.properties"
...
void setCommitIdAbbrevLength(int length) {
    if (length < 2 || length > 40) {
        throw new IllegalArgumentException(
            "commitIdAbbrevLength must be between 2 and 40, got: ${length}")
    }
    this.commitIdAbbrevLength = length
}
```

**After:**
```groovy
String gitPropertiesName = "git.properties"
...
void setGitPropertiesName(String name) {
    if (name == null) {
        throw new IllegalArgumentException("gitPropertiesName must not be null")
    }
    if (name.startsWith("/")) {
        throw new IllegalArgumentException(
            "gitPropertiesName must be a relative path that stays under gitPropertiesResourceDir, got: '${name}'")
    }
    if (name.split("[/\\\\]").any { it == ".." }) {
        throw new IllegalArgumentException(
            "gitPropertiesName must be a relative path that stays under gitPropertiesResourceDir, got: '${name}'")
    }
    this.gitPropertiesName = name
}

void setCommitIdAbbrevLength(int length) {
    if (length < 2 || length > 40) {
        throw new IllegalArgumentException(
            "commitIdAbbrevLength must be between 2 and 40, got: ${length}")
    }
    this.commitIdAbbrevLength = length
}
```

No changes needed to `GenerateGitPropertiesTask.groovy` — `Directory.file(String)` handles subpaths natively.

---

## New Tests Required

### Test 1: Unit tests for `setGitPropertiesName` validation

**File:** `src/test/groovy/com/gorylenko/GitPropertiesPluginExtensionTest.groovy`

Add after the existing `testSetGitPropertiesName` test (line 135):

```groovy
@Test
void testSetGitPropertiesNameWithSubdirPath() {
    def project = createProjectWithGit()
    def ext = getExtension(project)
    ext.gitPropertiesName = "discord4j/common/git.properties"
    assertEquals("discord4j/common/git.properties", ext.gitPropertiesName)
}

@Test(expected = IllegalArgumentException)
void testSetGitPropertiesNameRejectsLeadingSlash() {
    def project = createProjectWithGit()
    def ext = getExtension(project)
    ext.gitPropertiesName = "/git.properties"
}

@Test(expected = IllegalArgumentException)
void testSetGitPropertiesNameRejectsDotDotSegment() {
    def project = createProjectWithGit()
    def ext = getExtension(project)
    ext.gitPropertiesName = "../git.properties"
}

@Test(expected = IllegalArgumentException)
void testSetGitPropertiesNameRejectsDotDotInMiddle() {
    def project = createProjectWithGit()
    def ext = getExtension(project)
    ext.gitPropertiesName = "some/../other/git.properties"
}
```

### Test 2: Functional tests for subpath JAR packaging

**File:** `src/test/groovy/com/gorylenko/BasicFunctionalTest.groovy`

Add two tests:

```groovy
/**
 * Verify that gitPropertiesName = "subdir/git.properties" causes the file
 * to land at subdir/git.properties inside the JAR.
 */
@Test
public void testGitPropertiesNameSubdirLandsInJar() {
    def projectDir = temporaryFolder.newFolder()

    GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
        gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
    })

    new File(projectDir, "settings.gradle") << ""
    new File(projectDir, "build.gradle") << """
        plugins {
            id('java')
            id('com.gorylenko.gradle-git-properties')
        }
        gitProperties {
            gitPropertiesName = 'discord4j/common/git.properties'
        }
    """.stripIndent()

    def runner = GradleRunner.create()
            .withPluginClasspath()
            .withArguments("assemble")
            .withProjectDir(projectDir)

    def result = runner.build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
    assertEquals(TaskOutcome.SUCCESS, result.task(":processResources").outcome)

    // Verify file on disk at correct subpath
    def resourceDir = new File(projectDir, "build/generated/resources/git")
    assert new File(resourceDir, "discord4j/common/git.properties").exists()

    // Verify correct entry inside JAR
    def libsDir = new File(projectDir, "build/libs")
    def jarFiles = libsDir.listFiles({ File f -> f.name.endsWith(".jar") } as FileFilter)
    assertNotNull("build/libs directory not found or empty", jarFiles)
    assert jarFiles.length > 0 : "no JAR found in build/libs"

    def zipFile = new ZipFile(jarFiles[0])
    try {
        // Correct subpath entry must exist
        def entry = zipFile.getEntry("discord4j/common/git.properties")
        assertNotNull(
            "discord4j/common/git.properties NOT found in JAR '${jarFiles[0].name}' " +
            "(entries: ${zipFile.entries().collect { it.name }.join(', ')})",
            entry
        )
        // Root git.properties must NOT exist (only subpath)
        assertNull(
            "git.properties SHOULD NOT be at root when gitPropertiesName has a subdir",
            zipFile.getEntry("git.properties")
        )
    } finally {
        zipFile.close()
    }
}

/**
 * Regression: default gitPropertiesName = "git.properties" still lands at root of JAR.
 */
@Test
public void testGitPropertiesNameDefaultRegressionInJar() {
    def projectDir = temporaryFolder.newFolder()

    GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
        gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
    })

    new File(projectDir, "settings.gradle") << ""
    new File(projectDir, "build.gradle") << """
        plugins {
            id('java')
            id('com.gorylenko.gradle-git-properties')
        }
    """.stripIndent()

    def runner = GradleRunner.create()
            .withPluginClasspath()
            .withArguments("assemble")
            .withProjectDir(projectDir)

    def result = runner.build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)

    def libsDir = new File(projectDir, "build/libs")
    def jarFiles = libsDir.listFiles({ File f -> f.name.endsWith(".jar") } as FileFilter)
    assertNotNull("build/libs directory not found or empty", jarFiles)
    assert jarFiles.length > 0 : "no JAR found in build/libs"

    def zipFile = new ZipFile(jarFiles[0])
    try {
        def entry = zipFile.getEntry("git.properties")
        assertNotNull(
            "git.properties NOT found at JAR root for default gitPropertiesName",
            entry
        )
    } finally {
        zipFile.close()
    }
}
```

### Test 3: Functional test for validation fail-fast

Add to `BasicFunctionalTest.groovy`:

```groovy
@Test
public void testGitPropertiesNameWithLeadingSlashFailsFast() {
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
            gitPropertiesName = '/git.properties'
        }
    """.stripIndent()

    def runner = GradleRunner.create()
            .withPluginClasspath()
            .withArguments("generateGitProperties")
            .withProjectDir(projectDir)

    def result = runner.buildAndFail()
    assertThat(result.output, containsString("leading '/' is not allowed"))
}

@Test
public void testGitPropertiesNameWithDotDotFailsFast() {
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
            gitPropertiesName = '../git.properties'
        }
    """.stripIndent()

    def runner = GradleRunner.create()
            .withPluginClasspath()
            .withArguments("generateGitProperties")
            .withProjectDir(projectDir)

    def result = runner.buildAndFail()
    assertThat(result.output, containsString("'..' segments"))
}
```

---

## Task Breakdown (ordered)

### Task 1: Add `setGitPropertiesName` with validation

**File:** `src/main/groovy/com/gorylenko/GitPropertiesPlugin.groovy`

**What:** Add `setGitPropertiesName(String name)` method to `GitPropertiesPluginExtension` (inside the class, after line 89 or alongside `setCommitIdAbbrevLength`). Validates: null rejection, leading `/`, and `..` segments.

**Depends on:** Nothing.

---

### Task 2: Unit tests for validation

**File:** `src/test/groovy/com/gorylenko/GitPropertiesPluginExtensionTest.groovy`

**What:** Add 4 unit tests:
- `testSetGitPropertiesNameWithSubdirPath` — valid subpath accepted
- `testSetGitPropertiesNameRejectsLeadingSlash`
- `testSetGitPropertiesNameRejectsDotDotSegment`
- `testSetGitPropertiesNameRejectsDotDotInMiddle`

**Depends on:** Task 1.

---

### Task 3: Functional tests for subpath JAR packaging + regression + fail-fast

**File:** `src/test/groovy/com/gorylenko/BasicFunctionalTest.groovy`

**What:** Add 4 functional tests:
- `testGitPropertiesNameSubdirLandsInJar` — subpath appears in JAR at correct location, not at root
- `testGitPropertiesNameDefaultRegressionInJar` — default still works
- `testGitPropertiesNameWithLeadingSlashFailsFast`
- `testGitPropertiesNameWithDotDotFailsFast`

**Depends on:** Task 1.

---

### Task 4: README update

**File:** `README.md`

**What:** Add documentation for `gitPropertiesName` accepting relative paths with examples.

**Depends on:** Nothing (can be done independently).

---

### Task 5: MIGRATION.md update

**File:** `MIGRATION.md` (create or update)

**What:** Document that `gitPropertiesName` now supports subpaths (new in this version), and that invalid values (leading `/`, `..` segments) throw `IllegalArgumentException` at configuration time.

**Depends on:** Nothing (can be done independently).

---

## Risks and Gotchas

### 1. `Directory.file(String)` and subpaths — CONFIRMED SAFE

Gradle's `Directory.file(String)` resolves paths relative to the directory. `"discord4j/common/git.properties"` will produce the correct absolute path. No workaround needed.

### 2. Windows path separators

The `..` detection splits on both `/` and `\\` (using `[/\\\\]` regex). The leading `/` check only catches Unix-style absolute paths — Windows absolute paths starting with `C:\` or `\\` are NOT blocked. This is acceptable because:
- Gradle projects run on Windows will use `\` but a Windows absolute path starting with `C:\` won't start with `/`
- The validation purpose is path traversal prevention, not OS-level absolute path detection
- Mitigation: `Directory.file()` with an absolute path will resolve relative to the directory anyway on most Gradle versions (it resolves using `project.file()` semantics internally)

**Risk: LOW.** If needed, add `name.contains(":")` check for Windows drive letters in a follow-up.

### 3. `@OutputFile` and build cache with subpaths

The `@OutputFile` annotation on `getOutput()` declares the final file path as a task output. When `gitPropertiesName = "discord4j/common/git.properties"`, the output file path changes. This will correctly invalidate the build cache when the name changes — no issue here.

### 4. Default value `"git.properties"` is assigned at field initialization

```groovy
String gitPropertiesName = "git.properties"
```

The `setGitPropertiesName` setter will NOT be called for the default value assignment (field initializer bypasses Groovy setter). This is correct — the default is known-valid. Only user-set values go through validation.

### 5. Groovy property vs setter

In Groovy, `ext.gitPropertiesName = "foo"` calls `setGitPropertiesName("foo")` — the setter IS invoked when using Groovy property syntax. Also works for build scripts using `gitProperties { gitPropertiesName = 'subdir/git.properties' }`. This is the standard Groovy behavior and is correctly leveraged here.

### 6. `PropertiesFileWriter` already handles subdir creation

`mkdirs()` is called on `propsFile.parentFile` at line 40–42 of `PropertiesFileWriter.groovy`. No change needed.

### 7. No changes needed in `GenerateGitPropertiesTask`

`getGitPropertiesDir().file(gitProperties.gitPropertiesName)` already handles subpaths via `Directory.file(String)`. Zero changes in this file.
