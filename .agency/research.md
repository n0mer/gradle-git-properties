# Abbrev Length Research for Issue #234

## Current Implementation

**Hardcoded locations (value: 7):**
- `GitFacade.groovy:206` - `objectReader.abbreviate(revCommit.id, 7).name()`
- `GitTagService.groovy:61` - `objectReader.abbreviate(revCommit.id, 7).name()`

**Data flow:**
1. `GitPropertiesPluginExtension` - holds config (no abbrevLength yet)
2. `GenerateGitPropertiesTask` - calls `GitProperties.generate()`
3. `GitProperties.generate()` - opens `GitFacade`, evaluates property closures
4. `CommitIdAbbrevProperty.doCall()` - returns `facade.head().abbreviatedId`
5. `GitFacade.head()` - calls `toGitCommit()` which uses hardcoded 7
6. `GitCommit.abbreviatedId` - stores the abbreviated value

**Pattern for config properties:**
- Simple fields in `GitPropertiesPluginExtension` with defaults: `dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"`, `failOnNoGitDirectory = true`
- Passed through `GitProperties.generate()` params to property classes
- See `CommitTimeProperty(dateFormat, dateFormatTimeZone)` for example

## Git Standards

**core.abbrev:**
- Default: "auto" - computed based on object count for uniqueness
- In practice: 7 chars for small repos, more for large repos
- Min: 4 (git enforces minimum of 4)
- Max: 40 (full SHA-1)

**git log --abbrev=N:**
- Tested: `--abbrev=4` works (outputs 4 chars)
- Tested: `--abbrev=1` outputs 4 chars (enforced min)
- Value is minimum - git may output longer for uniqueness

## Maven git-commit-id-plugin

**Property:** `abbrevLength`
**Default:** 7
**Range:** 2-40 (validated, throws exception outside range)
**Behavior:** Minimum length, may be longer for uniqueness

```xml
<configuration>
  <abbrevLength>8</abbrevLength>
</configuration>
```

## JGit API

**Method:** `ObjectReader.abbreviate(AnyObjectId objectId, int len)`
- `len`: minimum length [2, 40]
- Returns: AbbreviatedObjectId that can be resolved back uniquely
- Auto-extends if needed for uniqueness

**Constant:** `Constants.OBJECT_ID_ABBREV_STRING_LENGTH` = 7 (default)

## Recommended Approach

### Extension Property
```groovy
class GitPropertiesPluginExtension {
    int commitIdAbbrevLength = 7  // or: abbrevLength = 7
}
```

Property name options:
- `commitIdAbbrevLength` - specific, matches `git.commit.id.abbrev`
- `abbrevLength` - matches Maven plugin

Recommend: `commitIdAbbrevLength` for clarity (more specific than Maven's)

### Validation
- Min: 2 (JGit requirement)
- Max: 40 (full SHA-1)
- Validate in extension setter or at generation time

### Implementation Changes

1. **GitPropertiesPluginExtension** - add `int commitIdAbbrevLength = 7`

2. **GitProperties.generate()** - add param, pass to facade/property

3. **GitFacade** - add abbrevLength param to:
   - `toGitCommit(RevCommit, int abbrevLength)`
   - Or make configurable via setter/constructor

4. **GitTagService** - same pattern

5. **CommitIdDescribeProperty** - uses `abbreviatedId`, needs access to length

### Alternative: Centralize in GitFacade

Instead of passing through all layers:
```groovy
class GitFacade {
    private int abbrevLength = 7
    
    static GitFacade open(File directory, int abbrevLength = 7) {
        def facade = new GitFacade(...)
        facade.abbrevLength = abbrevLength
        return facade
    }
}
```

This keeps the config change localized to one place.

### Test Cases
- Default behavior unchanged (7 chars)
- Custom length respected (e.g., 10)
- Edge cases: 2 (min), 40 (max/full), invalid values throw
