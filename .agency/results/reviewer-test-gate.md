# Test Gate Review

**Reviewer:** Claude Code
**Date:** 2026-05-21
**Input:** tester.md (26 scenarios, 23 pass, 2 fail, 1 skip)

---

## Coverage Assessment

**Alex:**

Coverage is solid for core functionality:
- Commit handling (basic, multiple, merge)
- Tag handling (lightweight, annotated, commits-after-tag)
- Branch handling (feature, slash, detached-head)
- Dirty state (staged, unstaged, untracked)
- Remote configuration (HTTPS, SSH)
- Configuration options (custom keys, date format, extra properties)
- Edge cases (unicode, long messages, no-git scenarios)
- Worktree (explicit dotGitDirectory)

**Gap identified:** No scenario for shallow clones. CI systems often use `git clone --depth=1`. This affects:
- `git.closest.tag.name` (may not find tags)
- `git.total.commit.count` (inaccurate)

Not a blocker for this gate, but should be added.

---

## Failure 1: 11-detached-head

**Alex:**

Categorization **correct**. This is a real parity issue.

Code verification confirms:
- v2.5.7: `repo.branch.current().name` returns `"HEAD"` for detached state (Grgit behavior)
- v3.0.0: `repository.branch` returns full SHA (JGit behavior)

Line 44 in `GitBranch.groovy`:
```groovy
return repository.branch  // Returns SHA when detached
```

**Severity:** Medium. CI/CD pipelines parsing `git.branch` will see different values.

**Fix is simple:** Check `repository.exactRef("HEAD").isSymbolic()` - if false, return `"HEAD"`.

---
**Dmitri:** Agree. The behavior change is observable. Users who deploy based on `git.branch != "HEAD"` will break. Fix before release.

[confidence: high - verified in code]

---

## Failure 2: 15-custom-output-location

**Alex:**

Categorization **correct**. This is a test harness bug, not a parity issue.

Error message proves it:
```
Cannot set DirectoryProperty using GStringImpl
```

The template uses:
```groovy
gitPropertiesDir = "${project.buildDir}/custom-output"  // GString
```

Should be:
```groovy
gitPropertiesDir = layout.buildDirectory.dir("custom-output")  // DirectoryProperty
```

Both v2.5.7 and v3.0.0 use `DirectoryProperty`. The test template is wrong, not the plugin.

---
**Dmitri:** Agree. But this masks potential parity issues in custom output. Fix harness, re-run before proceeding.

[confidence: high - error message is explicit]

---

## Verdict

| Question | Answer |
|----------|--------|
| 23 passing sufficient? | **Yes** - covers all major functionality |
| detached-head correctly categorized? | **Yes** - confirmed real parity issue |
| custom-output-location correctly categorized? | **Yes** - harness bug, not plugin |
| Fix harness before proceeding? | **Yes** - must verify custom output works |

### Action Required Before Verification Gate

1. **Fix detached HEAD parity issue** in `GitBranch.groovy`
2. **Fix test harness** template for custom output location
3. **Re-run test suite** to confirm:
   - 11-detached-head passes
   - 15-custom-output-location passes
   - No regressions

---

**SIGNAL: BLOCKED** - detached HEAD parity issue requires fix before v3.0.0 release
