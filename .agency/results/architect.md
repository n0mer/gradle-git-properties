# Test Harness Architecture: JGit Migration Validation

## Overall Approach

**Single shared git repo per scenario** - Both plugin versions run against the same `.git` directory. Eliminates git state differences.

**Declarative scenario definitions** - Each scenario is a YAML/shell config specifying:
1. Git state setup commands
2. Build.gradle template variant
3. Expected behavior flags

**Layered comparison** - Filter known-acceptable differences (timestamps, ordering) before diff.

## Directory Structure

```
/tmp/git-props-validation/
  gradlew -> /Users/I553838/projects/gradle-git-properties/gradlew
  lib/
    common.sh              # Shared functions
    templates/             # Build.gradle templates
      groovy-basic.gradle
      groovy-custom-props.gradle
      groovy-subset-keys.gradle
      kotlin-basic.gradle.kts
      kotlin-custom-props.gradle.kts
  scenarios/
    01-clean-with-tags/
      setup.sh             # Git state setup script
      config.env           # Scenario-specific config (template, flags)
      repo/                # Shared git repo
      old/                 # 2.5.7 project dir
        build.gradle
        settings.gradle
      new/                 # 3.0.0 project dir
        build.gradle
        settings.gradle
    02-clean-no-tags/
      ...
  results/
    summary.txt            # Overall pass/fail
    01-clean-with-tags.diff
    ...
  run-all.sh               # Main orchestrator
  run-scenario.sh          # Single scenario runner
  compare.sh               # Comparison logic
  setup-all.sh             # Create all 26 scenarios
```

## Key Scripts

### 1. `lib/common.sh`
Shared functions:
- `create_scenario_dirs(name)` - Creates repo/old/new structure
- `setup_git_repo(dir)` - Initialize git repo with user config
- `apply_template(template, version, target_dir)` - Copy and interpolate build.gradle
- `filter_props(input, output)` - Remove comments, sort, strip timestamps
- `run_gradle(project_dir, task)` - Execute gradle task

### 2. `setup-all.sh`
Creates all 26 scenarios by calling setup script for each.
```bash
for scenario in scenarios/*/; do
  source "$scenario/setup.sh"
done
```

### 3. `run-scenario.sh <scenario-name>`
1. Load `scenarios/<name>/config.env`
2. Run old version: `$GRADLEW -p old generateGitProperties`
3. Run new version: `$GRADLEW -p new generateGitProperties`
4. Compare with `compare.sh`

### 4. `run-all.sh`
```bash
for scenario in scenarios/*/; do
  ./run-scenario.sh "$(basename $scenario)"
done
# Generate summary
```

### 5. `compare.sh <old-props> <new-props> <output-diff>`
1. Filter both files (remove `#`, sort, exclude `git.build.time`)
2. diff -u
3. Return exit code (0=pass, 1=fail)

## Scenario Setup Requirements

### Phase 1: Core Properties

| Scenario | Git Setup |
|----------|-----------|
| 01-clean-with-tags | Commit file, add annotated tag `v1.0.0` |
| 02-clean-no-tags | Commit file only |
| 03-dirty-staged | Commit, then `git add` modified file |
| 04-dirty-unstaged | Commit, then modify tracked file |
| 05-dirty-untracked | Commit, then create untracked file |
| 06-detached-head | Commit, `git checkout HEAD~0` |
| 07-lightweight-tags | Commit, `git tag v1.0.0` (no -a) |

### Phase 2: Configuration Options

| Scenario | Config Variation |
|----------|------------------|
| 08-custom-filename | `gitPropertiesName = "build-info.properties"` |
| 09-custom-dir | `gitPropertiesResourceDir = file('custom-dir')` |
| 10-subset-keys | `keys = ['git.branch', 'git.commit.id']` |
| 11-date-formats | `dateFormat = "yyyy-MM-dd", dateFormatTimeZone = "UTC"` |
| 12-branch-override | `branch = "override-branch"` |
| 13-ext-property | `extProperty = 'gitProps'` + task to print |
| 14-fail-on-missing | No .git, `failOnNoGitDirectory = false` |

### Phase 3: Custom Properties

| Scenario | Custom Properties |
|----------|-------------------|
| 15-custom-static | `customProperty 'static.key', 'static-value'` |
| 16-custom-closure | `customProperty 'head.id', { it.head().id }` |
| 17-api-methods | Multiple closures testing all GitFacade methods |
| 18-override-standard | `customProperty 'git.commit.id.describe', 'override'` |
| 19-escape-hatch | `{ it.jgit.branch }` and `{ it.jgitCommands.status() }` |

### Phase 4: Special Scenarios

| Scenario | Setup |
|----------|-------|
| 20-shallow-clone | Use bundled zip or `git clone --depth=1 file://...` |
| 21-worktree | Create main repo, `git worktree add` |
| 22-submodule | Create parent, add submodule |
| 23-empty-repo | `git init` only, no commits |
| 24-multi-project | Root + subproject, both apply plugin |

### Phase 5: DSL Variants

| Scenario | Setup |
|----------|-------|
| 25-kotlin-dsl | Use `.kts` extension, Kotlin syntax |
| 26-kotlin-closure | `customProperty("key", KotlinClosure1({ it.head().id }, this))` |

## Build.gradle Templates

### Template 1: `groovy-basic.gradle`
```groovy
plugins {
    id 'java'
    id 'com.gorylenko.gradle-git-properties' version '${VERSION}'
}
gitProperties {
    dotGitDirectory = file("../repo/.git")
}
```

### Template 2: `groovy-custom-props.gradle`
```groovy
plugins {
    id 'java'
    id 'com.gorylenko.gradle-git-properties' version '${VERSION}'
}
gitProperties {
    dotGitDirectory = file("../repo/.git")
    customProperty('head.id') { it.head().id }
    customProperty('head.abbrev') { it.head().abbreviatedId }
    customProperty('branch.name') { it.branch.current().name }
    customProperty('status.clean') { it.status().clean.toString() }
    customProperty('describe.result') { it.describe() ?: '' }
    customProperty('log.count') { it.log().size().toString() }
    customProperty('tag.count') { it.tag.list().size().toString() }
}
```

### Template 3: `groovy-subset-keys.gradle`
```groovy
plugins {
    id 'java'
    id 'com.gorylenko.gradle-git-properties' version '${VERSION}'
}
gitProperties {
    dotGitDirectory = file("../repo/.git")
    keys = ['git.branch', 'git.commit.id', 'git.commit.id.abbrev']
}
```

### Template 4: `kotlin-basic.gradle.kts`
```kotlin
plugins {
    java
    id("com.gorylenko.gradle-git-properties") version "${VERSION}"
}
gitProperties {
    dotGitDirectory.set(file("../repo/.git"))
}
```

### Template 5: `kotlin-custom-props.gradle.kts`
```kotlin
import org.gradle.kotlin.dsl.KotlinClosure1

plugins {
    java
    id("com.gorylenko.gradle-git-properties") version "${VERSION}"
}
gitProperties {
    dotGitDirectory.set(file("../repo/.git"))
    customProperty("head.id", KotlinClosure1<Any, String>({ 
        (this as com.gorylenko.jgit.GitFacade).head().id 
    }, this))
}
```

### Additional templates for each config variation (6-7 more)

## Comparison and Reporting

### Filter Rules
1. Remove comment lines (starting with `#`)
2. Sort by key (properties file order is not guaranteed)
3. Exclude `git.build.time` (always differs)
4. Normalize line endings (CRLF -> LF)

### Output
- Per-scenario: `results/<scenario>.diff` (only if failed)
- Summary: `results/summary.txt`
```
PASS: 01-clean-with-tags
PASS: 02-clean-no-tags
FAIL: 03-dirty-staged (see results/03-dirty-staged.diff)
...
25/26 PASSED, 1/26 FAILED
```

### Exit Codes
- `run-scenario.sh`: 0=pass, 1=fail
- `run-all.sh`: 0=all pass, 1=at least one fail

## Task Breakdown for Implementer

### Task 1: Foundation (lib/common.sh)
- [ ] Implement all shared functions
- [ ] Test isolation of git operations

### Task 2: Templates (lib/templates/)
- [ ] Create all 8-10 build.gradle templates
- [ ] Test variable interpolation works

### Task 3: Phase 1 Scenarios (01-07)
- [ ] Create setup.sh and config.env for each
- [ ] Verify git states are correct
- [ ] Run baseline comparison

### Task 4: Phase 2 Scenarios (08-14)
- [ ] Create setup.sh and config.env for each
- [ ] Handle special cases (no .git for scenario 14)

### Task 5: Phase 3 Scenarios (15-19)
- [ ] Create custom property templates
- [ ] Verify API method coverage

### Task 6: Phase 4 Scenarios (20-24)
- [ ] Shallow clone setup (consider bundled zip)
- [ ] Worktree and submodule setup
- [ ] Multi-project setup

### Task 7: Phase 5 Scenarios (25-26)
- [ ] Kotlin DSL templates
- [ ] Test KotlinClosure syntax

### Task 8: Orchestration (run-all.sh, run-scenario.sh)
- [ ] Implement execution flow
- [ ] Add timing and progress output

### Task 9: Comparison (compare.sh)
- [ ] Implement filter rules
- [ ] Generate summary report

### Task 10: Documentation
- [ ] README for test harness
- [ ] Instructions for adding new scenarios

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Shallow clone setup complex | Bundle zip file like existing tests |
| Kotlin closure API differences | Test incrementally, may need conditional |
| Worktree/submodule flaky on CI | Run locally first, add retry logic |
| Git version differences | Document minimum git version |

---

**Dmitri:**

Design is sound. Shared repo per scenario is correct - eliminates state drift. Template approach is right abstraction level.

One concern: Phase 3 scenarios (custom properties) depend on GitFacade API parity. If API differs between versions, comparison will fail for legitimate reasons. Document expected API differences upfront.

Minor: `config.env` adds indirection. Could inline in `setup.sh` - fewer files to maintain. Not blocking.

[confidence: high - standard test harness pattern]

---

SIGNAL: TASK_DONE
