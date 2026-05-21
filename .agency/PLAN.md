# Implementation Plan: JGit Migration Validation

## Summary

Create a test harness with 26 scenarios comparing gradle-git-properties v2.5.7 (Grgit) vs v3.0.0 (JGit) output.

## Key Design Decisions

1. **Single shared git repo per scenario** - Both plugin versions run against same `.git` directory
2. **Template-based build.gradle** - Reusable templates with version interpolation
3. **Layered comparison** - Filter timestamps/ordering before diff
4. **Shell-based orchestration** - Simple, portable, no extra dependencies

## Known API Differences (Skip Comparison)

- Scenario 19 (escape-hatch): `jgit` and `jgitCommands` are v3.0.0 only - test v3.0.0 works, but can't compare to v2.5.7
- GitFacade in v2.5.7 wraps Grgit, in v3.0.0 wraps JGit - method names should match but verify

## Directory Structure

```
/tmp/git-props-validation/
├── lib/
│   ├── common.sh           # Shared functions
│   └── templates/          # Build.gradle templates (10 files)
├── scenarios/              # 26 scenario directories
│   └── NN-name/
│       ├── setup.sh        # Git state setup
│       ├── repo/           # Shared git repo
│       ├── old/            # v2.5.7 project
│       └── new/            # v3.0.0 project
├── results/                # Diff outputs
├── run-all.sh              # Main orchestrator
├── run-scenario.sh         # Single scenario runner
└── compare.sh              # Comparison logic
```

## Task Breakdown

### Task 1: Foundation
**Role:** Implementer  
**Files:** `lib/common.sh`, directory structure

Create shared functions:
- `create_scenario(name)` - Creates directory structure
- `init_git_repo(dir)` - Initialize git with user config
- `apply_template(template, version, target)` - Generate build.gradle
- `run_gradle(dir)` - Execute generateGitProperties

### Task 2: Templates  
**Role:** Implementer  
**Files:** `lib/templates/*.gradle`, `lib/templates/*.gradle.kts`

Create 10 templates:
1. `groovy-basic.gradle` - Standard config
2. `groovy-custom-filename.gradle` - Custom output filename
3. `groovy-custom-dir.gradle` - Custom output directory
4. `groovy-subset-keys.gradle` - Limited property keys
5. `groovy-date-format.gradle` - Custom date format/timezone
6. `groovy-branch-override.gradle` - Explicit branch name
7. `groovy-ext-property.gradle` - Expose to project.ext
8. `groovy-fail-on-missing.gradle` - failOnNoGitDirectory=false
9. `groovy-custom-props.gradle` - Custom properties with closures
10. `kotlin-basic.gradle.kts` - Kotlin DSL variant

### Task 3: Phase 1 Scenarios (Core Properties)
**Role:** Implementer  
**Scenarios:** 01-07

| # | Name | Git Setup |
|---|------|-----------|
| 01 | clean-with-tags | Commit + annotated tag v1.0.0 + remote |
| 02 | clean-no-tags | Commit only |
| 03 | dirty-staged | Commit + staged change |
| 04 | dirty-unstaged | Commit + modified file |
| 05 | dirty-untracked | Commit + untracked file |
| 06 | detached-head | Commit + checkout SHA |
| 07 | lightweight-tags | Commit + lightweight tag |

### Task 4: Phase 2 Scenarios (Config Options)
**Role:** Implementer  
**Scenarios:** 08-14

| # | Name | Template |
|---|------|----------|
| 08 | custom-filename | groovy-custom-filename |
| 09 | custom-dir | groovy-custom-dir |
| 10 | subset-keys | groovy-subset-keys |
| 11 | date-formats | groovy-date-format |
| 12 | branch-override | groovy-branch-override |
| 13 | ext-property | groovy-ext-property |
| 14 | fail-on-missing | groovy-fail-on-missing (no .git) |

### Task 5: Phase 3 Scenarios (Custom Properties)
**Role:** Implementer  
**Scenarios:** 15-19

| # | Name | Note |
|---|------|------|
| 15 | custom-static | Static string value |
| 16 | custom-closure | `{ it.head().id }` |
| 17 | api-methods | All GitFacade methods |
| 18 | override-standard | Override git.commit.id.describe |
| 19 | escape-hatch | **v3.0.0 only** - verify works, skip comparison |

### Task 6: Phase 4 Scenarios (Special)
**Role:** Implementer  
**Scenarios:** 20-24

| # | Name | Setup Complexity |
|---|------|------------------|
| 20 | shallow-clone | Clone with --depth=1 |
| 21 | worktree | git worktree add |
| 22 | submodule | Parent + child repos |
| 23 | empty-repo | git init only |
| 24 | multi-project | Root + subproject |

### Task 7: Phase 5 Scenarios (Kotlin DSL)
**Role:** Implementer  
**Scenarios:** 25-26

| # | Name | Template |
|---|------|----------|
| 25 | kotlin-dsl | kotlin-basic.gradle.kts |
| 26 | kotlin-closure | KotlinClosure1 custom props |

### Task 8: Orchestration
**Role:** Implementer  
**Files:** `run-all.sh`, `run-scenario.sh`

- Run individual scenario: setup → old → new → compare
- Run all scenarios with progress output
- Generate summary report

### Task 9: Comparison Logic
**Role:** Implementer  
**Files:** `compare.sh`

Filter rules:
1. Remove comment lines (`#`)
2. Sort by property key
3. Exclude `git.build.time`
4. Normalize line endings

Output: Pass/fail with diff on failure

### Task 10: Run Full Suite
**Role:** Tester  

Execute all 26 scenarios and report results.

## Verification

1. All 26 scenarios created and runnable
2. Scenarios 01-18, 20-26 produce identical output (PASS)
3. Scenario 19 (escape-hatch) runs successfully on v3.0.0 (skip comparison)
4. Any differences are documented and justified

## Timeline

- Tasks 1-2: Foundation (~30 min)
- Tasks 3-7: Scenarios (~1 hour)
- Tasks 8-9: Orchestration (~20 min)
- Task 10: Execution (~30 min)

Total: ~2.5 hours
