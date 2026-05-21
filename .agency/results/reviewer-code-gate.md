# Code Gate Review: Test Harness Implementation

**Reviewer:** Claude (Code Gate Role)  
**Date:** 2026-05-21  
**Artifact:** `/tmp/git-props-validation/`

---

## 1. Completeness Check

**Status:** PASS

All 26 scenarios implemented:

| # | Scenario | Template | Status |
|---|----------|----------|--------|
| 01 | basic-repo | basic | Present |
| 02 | multiple-commits | basic | Present |
| 03 | lightweight-tag | basic | Present |
| 04 | annotated-tag | basic | Present |
| 05 | commits-after-tag | basic | Present |
| 06 | feature-branch | basic | Present |
| 07 | branch-with-slash | basic | Present |
| 08 | dirty-staged | basic | Present |
| 09 | dirty-unstaged | basic | Present |
| 10 | untracked-files | basic | Present |
| 11 | detached-head | basic | Present |
| 12 | with-remote | basic | Present |
| 13 | ssh-remote | basic | Present |
| 14 | custom-keys | custom-keys | Present |
| 15 | custom-output-location | custom-output | Present |
| 16 | date-format | date-format | Present |
| 17 | custom-properties | custom-properties | Present |
| 18 | extra-property | extra-properties | Present |
| 19 | escape-hatch | basic (v3 only) | Present |
| 20 | fail-on-no-git | fail-on-no-git | Present |
| 21 | no-fail-on-no-git | no-fail-on-no-git | Present |
| 22 | explicit-dot-git | dot-git-directory | Present |
| 23 | force-generation | force-generation | Present |
| 24 | unicode-commit-message | basic | Present |
| 25 | long-commit-message | basic | Present |
| 26 | merge-commit | basic | Present |

Templates verified present: 10 templates in `/tmp/git-props-validation/lib/templates/`

---

## 2. Correctness Check

### Scripts Structure
- `lib/common.sh` - Well-organized shared functions with proper error handling (`set -euo pipefail`)
- `run-scenario.sh` - Correctly locates scenarios by partial match, calls comparison
- `compare.sh` - Handles both property comparison and behavior result files (for failure scenarios)
- `run-all.sh` - Proper orchestration with summary tracking

### Verification Results

| Scenario | Run Result |
|----------|------------|
| 01-basic-repo | **PASS** - Properties match |
| 11-detached-head | **FAIL** - Behavior difference detected (see below) |
| 17-custom-properties | **PASS** - Properties match |
| 19-escape-hatch | **PASS** - v3-only correctly skipped comparison |

---

## 3. Filter Logic Review

**Status:** PASS

The filter logic in `lib/common.sh` lines 182-188:

```bash
filter_props() {
    local input="$1"
    local output="$2"
    # Filter out git.build.time (always differs) and sort for consistent comparison
    grep -v '^git.build.time=' "$input" | grep -v '^#' | sort > "$output"
}
```

Correctly:
- Excludes `git.build.time=` (timestamp always differs)
- Excludes comment lines (`#`)
- Sorts for deterministic comparison

---

## 4. Scenario 19 (Escape-Hatch) Review

**Status:** PASS

The scenario is correctly marked as v3-only:

1. `lib/common.sh` line 201-204 defines `is_v3_only_scenario()`:
   ```bash
   is_v3_only_scenario() {
       local scenario="$1"
       [[ "$scenario" == "19-escape-hatch" ]]
   }
   ```

2. `scenarios/19-escape-hatch.sh` only runs v3.0.0 and includes validation
3. `compare.sh` lines 40-43 skip comparison for v3-only scenarios
4. `run-all.sh` correctly counts v3-only scenarios as SKIPPED (not failures)

---

## 5. Issues Found

### CRITICAL: Behavioral Difference in Detached HEAD

Scenario 11 revealed a real parity issue:

| Property | v2.5.7 (Grgit) | v3.0.0 (JGit) |
|----------|----------------|---------------|
| git.branch | `HEAD` | `<full commit SHA>` |

This is a **functional difference**, not a test harness bug. The harness correctly detected this parity issue.

**Impact:** Applications relying on `git.branch=HEAD` to detect detached state will break in v3.0.0.

**Recommendation:** This must be fixed in the JGit implementation before release. The harness is working as designed - it found a real bug.

### MINOR: Hardcoded Paths

`lib/common.sh` line 7 has hardcoded path:
```bash
GRADLEW="/Users/I553838/projects/gradle-git-properties/gradlew"
```

This is acceptable for the current validation but limits portability.

---

## 6. Summary

| Criterion | Status |
|-----------|--------|
| All 26 scenarios implemented | PASS |
| Scripts execute correctly | PASS |
| Filter excludes git.build.time | PASS |
| Scenario 19 marked v3-only | PASS |
| Detected real parity issues | YES (11-detached-head) |

---

## Decision

The test harness implementation is **correct and complete**. It successfully identified a real behavioral difference in scenario 11 (detached HEAD handling).

The failure of scenario 11 is not a test harness bug - it is the harness doing its job: detecting parity issues between v2.5.7 and v3.0.0.

**SIGNAL: PASS**

The implementation is acceptable. The detected parity issue (detached HEAD branch value) should be tracked separately for remediation in the JGit-based plugin.
