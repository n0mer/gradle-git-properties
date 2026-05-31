# Version Parity Validation Guide

Process for validating version parity between gradle-git-properties releases.

## Agent Execution Model

**IMPORTANT:** When using AI agents to perform this validation:

1. **Work as Coordinator** - The main agent must act as an orchestrator, NOT do heavy work itself
2. **Delegate to Subagents** - Spawn subagents for:
   - Feature discovery and research
   - Test harness creation
   - Running test suites
   - Analyzing failures
   - Fixing issues
3. **Follow Agency Skill** - Use the agency workflow pattern:
   - SPEC → PLAN → Human Gate → EXECUTE (with quality gates) → REFLECT → DOCUMENT
   - Sequential execution with reviewer gates between phases
4. **Follow Harness Mode** - For verification:
   - Fast feedback first (unit tests), then slow (integration)
   - Subagent isolation for heavy operations
   - Keep main context clean for coordination
5. **Verify Before Committing** - Always verify native behavior (e.g., `git rev-parse --abbrev-ref HEAD`) before assuming expected output
6. **Ask Before Fixing** - Discuss parity issues with user before applying fixes
7. **No Bias / Fresh Discovery** - Each validation run must start completely fresh:
   - **DELETE `/tmp/git-props-validation/` first** - Never "add to" existing harness
   - Read README and source code to discover ALL features
   - Design test scenarios based on YOUR discovery
   - Do NOT look at previous test results, scenarios, or harness code
   - Do NOT assume any specific number of tests
   - Build the entire test harness from scratch each time
8. **Test Harness is the CONTRACT** - Once created from README/docs, the harness is FINAL:
   - **NEVER modify tests to make them pass** - this is cheating
   - The harness represents the documented API contract
   - If a test fails, investigate as a potential PARITY ISSUE in the code
   - Only modify harness if you can prove a bug in test generation (not in the code under test)
   - When in doubt, ASK the user before changing any test
   - Changing tests to pass = hiding bugs, not fixing them

## Process Overview

### Phase 1: Feature Discovery

Discover all features by reading:
- `README.md` - User-facing documentation
- `src/main/groovy/com/gorylenko/` - Plugin implementation
- `src/main/groovy/com/gorylenko/properties/` - Property generators

Identify:
- All generated properties
- All configuration options
- Custom properties API
- Edge cases and special scenarios

### Phase 2: Test Matrix Design

Design test scenarios covering:
- Each property in various git states
- Each configuration option
- Custom properties API methods
- Edge cases (detached HEAD, no tags, dirty states, etc.)
- Error handling scenarios
- DSL variants (Groovy, Kotlin)

### Phase 3: Install Versions

```bash
# Old version
git checkout <old-tag-or-branch>
./gradlew publishPluginMavenPublicationToMavenLocal

# New version  
git checkout <new-tag-or-branch>
./gradlew publishPluginMavenPublicationToMavenLocal
```

Both install to `~/.m2/repository/com/gorylenko/gradle-git-properties/`.

### Phase 4: Create Test Harness

Create at `/tmp/git-props-validation/`:
- Scenario scripts that set up git state
- Build.gradle templates for each configuration
- Scripts to run both versions against same git repo
- Comparison logic

### Phase 5: Run and Compare

For each scenario:
1. Set up git state
2. Run old version → capture git.properties
3. Run new version → capture git.properties
4. Compare (filter acceptable differences like `git.build.time`)

### Phase 6: Analyze Failures

For each failure, determine:
- **Parity issue** - New version behaves differently (needs fix)
- **Test harness bug** - Both versions fail identically (fix test)
- **New feature** - Only exists in new version (skip comparison)

### Phase 7: Fix and Verify

For parity issues:
1. Verify expected behavior with native git commands
2. Discuss with user before fixing
3. Fix in new version
4. Rebuild and re-run

## Comparison Rules

Filter before comparing:
- Remove `git.build.time` (always differs)
- Remove comment lines (`#`)
- Sort alphabetically (order may vary)

## Target

100% parity on all comparable scenarios.

## Test Scenarios (77 total)

### Core Properties (01-14)
- `01-basic-properties` - Standard git.properties with all default keys
- `02-detached-head` - Detached HEAD state (checkout specific commit)
- `03-no-tags` - Repository with no tags
- `04-with-tags` - Repository with annotated tag on HEAD
- `05-dirty-state` - Uncommitted changes in working directory
- `06-custom-properties-static` - Static custom property values
- `07-custom-properties-closure` - Custom properties via closure
- `08-custom-properties-api` - Custom properties via GitRepositoryInfo API
- `09-selective-keys` - Subset of keys via `keys` configuration
- `10-date-format` - Custom dateFormat and dateFormatTimeZone
- `11-empty-repo` - Repository with no commits
- `12-multiple-tags-on-head` - Multiple tags pointing to same commit
- `13-multiline-message` - Commit message with newlines
- `14-custom-filename` - Custom gitPropertiesName

### Worktree Variants (15-28)
- `15-basic-properties-worktree` - Basic properties in git worktree
- `16-detached-head-worktree` - Detached HEAD in worktree
- `17-no-tags-worktree` - No tags in worktree
- `18-with-tags-worktree` - Tags in worktree
- `19-dirty-state-worktree` - Dirty state in worktree
- `20-custom-properties-static-worktree` - Static custom props in worktree
- `21-custom-properties-closure-worktree` - Closure custom props in worktree
- `22-custom-properties-api-worktree` - API custom props in worktree
- `23-selective-keys-worktree` - Selective keys in worktree
- `24-date-format-worktree` - Date format in worktree
- `25-empty-repo-worktree` - Empty repo in worktree
- `26-multiple-tags-on-head-worktree` - Multiple tags in worktree
- `27-multiline-message-worktree` - Multiline message in worktree
- `28-custom-filename-worktree` - Custom filename in worktree

### Special Git States (29-37)
- `29-shallow-clone` - Shallow clone (--depth=1)
- `30-remote-url-credentials` - Remote URL with embedded credentials (should be sanitized)
- `31-annotated-tag` - Annotated tag behavior
- `32-lightweight-tag` - Lightweight tag behavior
- `33-special-branch-names` - Branch names with special characters
- `34-unicode-commit-message` - Unicode in commit message
- `35-unicode-author` - Unicode in author name/email
- `36-merge-commit` - Merge commit properties
- `37-long-commit-message` - Very long commit message truncation

### Configuration Options (38-46)
- `38-failOnNoGitDirectory-false` - No git directory with failOnNoGitDirectory=false
- `39-ci-branch-override` - Branch override via gitProperties.branch
- `40-ci-branch-override-refs-prefix` - Branch override with refs/heads/ prefix
- `41-ci-branch-override-refs-tags` - Branch override with refs/tags/ prefix
- `42-ci-detached-no-override` - Detached HEAD without branch override
- `43-ci-branch-empty-string` - Branch override as empty string
- `44-ci-branch-null` - Branch override as null
- `45-ci-env-jenkins` - Jenkins CI environment simulation
- `46-ci-env-github-actions` - GitHub Actions environment simulation

### Auto-Detect Mode (47-64)
- `47-auto-detect-git-directory` - Auto-detect .git directory (no dotGitDirectory config)
- `48-auto-detect-git-worktree` - Auto-detect .git file in worktree
- `49-auto-detect-subdirectory` - Auto-detect from project subdirectory
- `50-auto-detect-worktree-subdirectory` - Auto-detect worktree from subdirectory
- `51-auto-detect-basic-properties` - Basic properties with auto-detect
- `52-auto-detect-detached-head` - Detached HEAD with auto-detect
- `53-auto-detect-no-tags` - No tags with auto-detect
- `54-auto-detect-with-tags` - With tags with auto-detect
- `55-auto-detect-dirty-state` - Dirty state with auto-detect
- `56-auto-detect-custom-properties-static` - Static custom props with auto-detect
- `57-auto-detect-custom-properties-closure` - Closure custom props with auto-detect
- `58-auto-detect-custom-properties-api` - API custom props with auto-detect
- `59-auto-detect-selective-keys` - Selective keys with auto-detect
- `60-auto-detect-date-format` - Date format with auto-detect
- `61-auto-detect-empty-repo` - Empty repo with auto-detect
- `62-auto-detect-multiple-tags-on-head` - Multiple tags with auto-detect
- `63-auto-detect-multiline-message` - Multiline message with auto-detect
- `64-auto-detect-custom-filename` - Custom filename with auto-detect

### GitFacade API Methods (65-73)
- `65-describe-long-format` - `it.describe(longDescr: true)` long describe output
- `66-log-api` - `it.log(maxCommits: 5)` commit history API
- `67-tag-list-api` - `it.tag.list()` all tags API
- `68-tag-list-on-commit-api` - `it.tag.listOnCommit(id)` tags on specific commit
- `69-tag-closest-api` - `it.tag.closest()` nearest tag with distance
- `70-get-config-api` - `it.getConfig(section, name)` git config reading
- `71-is-empty-api` - `it.isEmpty()` empty repository check
- `72-jgit-escape-hatch` - `it.jgit` raw JGit Repository access
- `73-jgit-commands-escape-hatch` - `it.jgitCommands` raw Git commands

### Additional Configuration (74-75)
- `74-ext-property` - `extProperty` exposing git props to `project.ext` (Issue #304)
  - `74a-ext-property-task-before-config` - task reference with `dependsOn generateGitProperties` appears **before** `gitProperties { extProperty = 'gitProps' }` block — the bug scenario from #304; must succeed and print branch name
  - `74b-ext-property-config-before-task` - `gitProperties { extProperty = 'gitProps' }` block appears **before** the task reference — safe ordering control test; must succeed and print branch name
  - `74c-ext-property-not-set` - `extProperty` not configured; `project.ext` must not have any extra key registered by the plugin
  - `74d-ext-property-config-cache` - configuration cache compatibility: `def ext = project.ext` captured as a local variable before `doLast` (correct pattern); accessing `project.ext` directly inside `doLast` may fail with config cache enabled and is not supported
- `75-force-write` - `force = true` always write file even if unchanged

### Edge Cases (76-77)
- `76-submodule` - Git submodule (.git file pointing to parent repo)
- `77-no-git-user-config` - No user.name/email in git config
