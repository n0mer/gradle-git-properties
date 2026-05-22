# SPEC: Configurable Commit ID Abbreviation Length

**Issue:** [#234](https://github.com/n0mer/gradle-git-properties/issues/234)
**Branch:** `fix/issue-234-abbrev-length`

## Problem

The `git.commit.id.abbrev` property is hardcoded to 7 characters. Users need configurable length (e.g., 8 chars for their workflow).

## Requirements

| Requirement | Detail |
|-------------|--------|
| Config property | `commitIdAbbrevLength` in `gitProperties {}` block |
| Default | 7 (Git standard) |
| Range | 2-40 (JGit constraint) |
| Validation | Fail fast at config time |
| Backward compat | `it.head().abbreviatedId` in custom properties stays at 7 |

## Usage

```groovy
gitProperties {
    commitIdAbbrevLength = 10
}
```

Output:
```properties
git.commit.id.abbrev=a1b2c3d4e5
```

## Design

Follow existing `dateFormat` pattern: thread config through all layers.

**Data flow:**
```
GitPropertiesPluginExtension.commitIdAbbrevLength = 10
    ↓
GenerateGitPropertiesTask (pass to generate())
    ↓
GitProperties.generate(..., commitIdAbbrevLength)
    ↓
CommitIdAbbrevProperty(abbrevLength)
    ↓
GitFacade.getAbbreviatedId(length)  ← NEW METHOD
```

## Files to Modify

1. `GitPropertiesPlugin.groovy` - add `int commitIdAbbrevLength = 7` with setter validation
2. `GenerateGitPropertiesTask.groovy` - pass new param to `generate()`
3. `GitProperties.groovy` - add param to `generate()` and `getStandardPropertiesMap()`
4. `CommitIdAbbrevProperty.groovy` - accept length in constructor, use `facade.getAbbreviatedId(length)`
5. `GitFacade.groovy` - add `getAbbreviatedId(int length)` method

## Test Cases

| Test | Validates |
|------|-----------|
| Default length | Returns 7 chars when not configured |
| Custom length 10 | Returns 10 chars |
| Min boundary (2) | Works with minimum valid length |
| Max boundary (40) | Returns full SHA |
| Invalid (1) | Throws at config time |
| Invalid (41) | Throws at config time |
| Functional test | End-to-end with `build.gradle` config |

## Out of Scope

- `GitTagService.toGitCommit()` - not used for abbrev property
- `GitCommit.abbreviatedId` field - keep at 7 for backward compat

## Acceptance Criteria

- [ ] `commitIdAbbrevLength` configurable in `gitProperties {}` block
- [ ] Default behavior unchanged (7 chars)
- [ ] Invalid values (< 2 or > 40) throw clear error
- [ ] All existing tests pass
- [ ] New tests for the feature
