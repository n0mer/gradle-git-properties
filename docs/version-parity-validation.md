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
7. **No Bias / Fresh Discovery** - Each validation run must discover features independently:
   - Read README and source code to discover ALL features
   - Design test scenarios based on YOUR discovery
   - Do NOT reuse previous test results or scenarios
   - Do NOT assume any specific number of tests

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
