# Sprint Reflection: gitPropertiesName Relative Path Support (v4.0.2)

**Date:** 2026-06-03
**Feature:** `gitPropertiesName` accepts relative paths to control JAR subpath placement
**Result:** All gates PASS first time. 306 → 319 tests, zero regressions.

---

## 1. What Worked Well

### Clean RED→GREEN discipline at the unit level
Task 1 (6 validation behaviors) executed TDD correctly:
- Behaviors 0–1 (plain filename, subpath acceptance): Groovy auto-setter handled these natively — no artificial RED needed, confirmed GREEN immediately.
- Behaviors 2–4 (null, leading `/`, `..` segments): each produced a genuine RED state because Groovy's auto-setter has no validation. Implementation was added incrementally per behavior.
- Behavior 5 (`..'` in middle): free GREEN from Behavior 4's implementation — the `segments.contains("..")` check is positional-agnostic.

This is textbook TDD: real RED where validation was missing, free GREEN where implementation generalized naturally.

### Unit-driven design made functional tests trivially GREEN
All 5 functional tests (Task 2) passed on first run with zero retries. The unit-level RED→GREEN cycle already drove the complete implementation. Functional tests acted as acceptance gates, not discovery vehicles — exactly the right role.

### Minimal API surface
The design choice to extend `gitPropertiesName` (relative path) rather than add a new property kept the API clean. No new config properties, no migration burden, backward-compatible. Existing `gitPropertiesName = "git.properties"` configs are completely unaffected.

### Parallel doc execution
Tasks 4–7 (README, MIGRATION.md, CHANGELOG, version bump) were written in parallel with no blocking dependencies. Correct scheduling.

---

## 2. Patterns to Repeat

### Groovy auto-setter rule
Any `GitPropertiesPluginExtension` field with input validation requirements needs an explicit `setSomething(String value)` method. Groovy auto-setters are invisible to custom validation — you will not get a RED state for invalid inputs without the explicit method.

### Extend before adding
Before proposing a new config property, ask: does this requirement fit an existing property's semantic? In this sprint, "control JAR path" fit cleanly into `gitPropertiesName` (the file name/path). No new property needed.

### Parity check as final gate
Running both v4.0.1 (306 tests) and v4.0.2 (319 tests) explicitly and comparing counts is a reliable final gate. The delta (13 tests) is fully accounted for by new tests — no regressions, no silent deletions.

---

## 3. What Could Have Gone Wrong (But Didn't)

### Path separator handling
The `..` segment check splits on `[/\\]` (both forward and backslash). On Windows-hosted Gradle builds, a user could theoretically supply `..\\git.properties`. The implementation handles this correctly, but it was not explicitly tested. If a Windows functional test is added later, this edge case is covered.

### Groovy field init vs setter
The default `String gitPropertiesName = "git.properties"` uses field initialization, not the setter. If the validator had been written with a null check that assumed the setter was called for the default, initialization would have silently bypassed it. The design correctly uses field init for the default and the setter only for user-supplied values.

### Overlapping outputs regression
The feature was motivated by users hitting `WorkValidationException` when pointing `gitPropertiesResourceDir` at a subdirectory of `build/resources/main/`. The new feature avoids this entirely by writing to `build/generated/resources/git/<subpath>` — the path stays within the plugin's designated output root. No overlap possible.

---

## 4. Design Insights

### Conflating "write location" and "JAR path" is a recurring plugin mistake
The root cause of issue #306 was that `gitPropertiesResourceDir` tried to control both where the file is written and where it appears in the JAR by using a single `srcDir()` call. When users pointed it at `build/resources/main/subdir/`, Gradle correctly flagged overlapping outputs. The fix separates the two concerns: `gitPropertiesResourceDir` stays as the write location root; `gitPropertiesName` (with relative path) controls the JAR subpath by controlling the file's position within that root.

### Path traversal validation belongs at the setter, not at task execution time
Validating `..` and leading `/` in the setter (at configuration time) rather than in `generateGitProperties` (at task execution time) produces a faster, clearer failure. Users see the error during configuration, before any task runs. This is the "fail fast" principle applied correctly.

### TDD scope: unit tests drive design, functional tests confirm it
The sprint demonstrated a clean separation of roles. Unit tests (Task 1) discovered the Groovy auto-setter gap and drove the explicit setter implementation. Functional tests (Task 2) confirmed the end-to-end JAR packaging behavior was correct. Neither task required the other to exist first — this is the right layering.

---

## Lessons Stored

Three lessons captured in lessons-learned (IDs: lesson-20260603-225008-001 through -003):

1. **Extend existing property semantics before adding new properties** — `gitPropertiesName` accepting relative paths vs adding a new `gitPropertiesSubpath` property.
2. **Groovy auto-setters bypass validation** — explicit setter method required for any field with null/path-traversal checks.
3. **Functional tests GREEN on first run = unit TDD was complete** — do not force artificial RED for functional tests when unit-level design was already sound.
