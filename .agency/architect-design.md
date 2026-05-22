# Design: Configurable Commit ID Abbreviation Length (Issue #234)

## Recommendation: Option A - Thread Through All Layers

**Rationale:**
- Follows existing pattern established by `dateFormat`/`dateFormatTimeZone`
- Maintains immutability of `GitCommit` (value object stays clean)
- Config flows predictably: Extension -> Task -> GitProperties -> Property class
- No hidden state in GitFacade

## Files to Modify

### 1. GitPropertiesPluginExtension (config holder)
**File:** `src/main/groovy/com/gorylenko/GitPropertiesPlugin.groovy`
```groovy
// Add after line 72 (dateFormatTimeZone)
int commitIdAbbrevLength = 7
```

### 2. GenerateGitPropertiesTask (pass config)
**File:** `src/main/groovy/com/gorylenko/GenerateGitPropertiesTask.groovy`
```groovy
// Modify line 99-101 (generate() call)
Map<String, String> newMap = builder.generate(dotGitDirectory,
    gitProperties.keys, gitProperties.dateFormat, gitProperties.dateFormatTimeZone, 
    gitProperties.branch, projectVersion.get(), gitProperties.customProperties,
    gitProperties.commitIdAbbrevLength)  // NEW PARAM
```

### 3. GitProperties (method signature)
**File:** `src/main/groovy/com/gorylenko/GitProperties.groovy`
```groovy
// Line 55: Add parameter
public Map<String, String> generate(File dotGitDirectory, List<String> keys, 
    String dateFormat, String dateFormatTimeZone, String branch,
    Object buildVersion, Map<String, Object> customProperties, 
    int commitIdAbbrevLength) {  // NEW PARAM

// Line 82: Pass to map builder
private static Map getStandardPropertiesMap(String dateFormat, String dateFormatTimeZone, 
    String branch, Object buildVersion, int commitIdAbbrevLength) {

// Line 87: Pass to CommitIdAbbrevProperty
, (KEY_GIT_COMMIT_ID_ABBREVIATED)    : new CommitIdAbbrevProperty(commitIdAbbrevLength)
```

### 4. CommitIdAbbrevProperty (accept config)
**File:** `src/main/groovy/com/gorylenko/properties/CommitIdAbbrevProperty.groovy`
```groovy
class CommitIdAbbrevProperty extends AbstractGitProperty {
    private final int abbrevLength

    CommitIdAbbrevProperty(int abbrevLength) {
        this.abbrevLength = abbrevLength
    }

    String doCall(GitFacade facade) {
        return isEmpty(facade) ? '' : facade.getAbbreviatedId(abbrevLength)
    }
}
```

### 5. GitFacade (parameterized abbreviation)
**File:** `src/main/groovy/com/gorylenko/jgit/GitFacade.groovy`
```groovy
// Add new method (around line 180, after head())
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

### 6. GitCommit (optional - keep for backward compat)
**File:** `src/main/groovy/com/gorylenko/jgit/GitCommit.groovy`
- Keep `abbreviatedId` field (still populated with 7 chars)
- Custom properties using `it.head().abbreviatedId` continue to work

## Validation Strategy

**Location:** `GitPropertiesPluginExtension` (fail fast at config time)

```groovy
void setCommitIdAbbrevLength(int length) {
    if (length < 2 || length > 40) {
        throw new IllegalArgumentException(
            "commitIdAbbrevLength must be between 2 and 40, got: ${length}")
    }
    this.commitIdAbbrevLength = length
}
```

Alternative: Validate in `GenerateGitPropertiesTask` at task execution time.

**Recommendation:** Extension (fail fast) - matches Gradle convention of validating config early.

## Test Cases Needed

### Unit Tests

1. **CommitIdAbbrevPropertyTest** (`src/test/groovy/com/gorylenko/properties/`)
   - `testDoCallWithDefaultLength()` - returns 7 chars
   - `testDoCallWithLength10()` - returns 10 chars
   - `testDoCallWithLength40()` - returns full SHA
   - `testDoCallWithLength2()` - returns 2 chars (minimum)

2. **GitFacadeTest** (`src/test/groovy/com/gorylenko/jgit/`)
   - `testGetAbbreviatedIdWithLength()` - verify parameterized abbreviation

3. **GitPropertiesPluginExtensionTest** (`src/test/groovy/com/gorylenko/`)
   - `testCommitIdAbbrevLengthDefault()` - verify default is 7
   - `testCommitIdAbbrevLengthValidation()` - verify 1 throws, 2 ok, 40 ok, 41 throws

### Functional Tests

4. **BasicFunctionalTest** or new file
   - `testCustomAbbrevLength()` - full integration with build.gradle config:
     ```groovy
     gitProperties {
         commitIdAbbrevLength = 10
     }
     ```
   - Verify output file contains 10-char abbreviation

## Alternative Considered: Option B (Store in GitFacade)

```groovy
// GitFacade would have mutable state
class GitFacade {
    int abbrevLength = 7
    static GitFacade open(File dir, int abbrevLength = 7)
}
```

**Rejected because:**
- Adds mutable state to GitFacade (currently immutable after construction)
- Factory method signature change affects all callers
- Less consistent with existing pattern (dateFormat doesn't go through facade)

## Impact on Custom Properties

Users with custom properties like:
```groovy
customProperty('my.abbrev') { it.head().abbreviatedId }
```

**Behavior:** Will continue to return 7 chars (from GitCommit field)

**If user wants configurable length in custom property:** 
```groovy
customProperty('my.abbrev') { it.getAbbreviatedId(10) }
```

## Summary

| Aspect | Choice |
|--------|--------|
| Pattern | Thread-through (like dateFormat) |
| Default | 7 (Git standard) |
| Validation | Extension setter, fail fast |
| Range | 2-40 (JGit constraint) |
| Backward compat | GitCommit.abbreviatedId unchanged |
