# Test Harness Implementation Summary

## Location
`/tmp/git-props-validation/`

## Structure

```
/tmp/git-props-validation/
├── run-all.sh          # Execute all 26 scenarios
├── run-scenario.sh     # Execute single scenario by name/number
├── compare.sh          # Compare v2.5.7 vs v3.0.0 output
├── lib/
│   ├── common.sh       # Shared functions (git setup, gradle execution)
│   └── templates/      # 10 build.gradle templates
│       ├── basic.gradle
│       ├── custom-keys.gradle
│       ├── custom-output.gradle
│       ├── custom-properties.gradle
│       ├── date-format.gradle
│       ├── dot-git-directory.gradle
│       ├── extra-properties.gradle
│       ├── fail-on-no-git.gradle
│       ├── force-generation.gradle
│       └── no-fail-on-no-git.gradle
├── scenarios/          # 26 scenario scripts
│   ├── 01-basic-repo.sh
│   ├── 02-multiple-commits.sh
│   ├── 03-lightweight-tag.sh
│   ├── 04-annotated-tag.sh
│   ├── 05-commits-after-tag.sh
│   ├── 06-feature-branch.sh
│   ├── 07-branch-with-slash.sh
│   ├── 08-dirty-staged.sh
│   ├── 09-dirty-unstaged.sh
│   ├── 10-untracked-files.sh
│   ├── 11-detached-head.sh
│   ├── 12-with-remote.sh
│   ├── 13-ssh-remote.sh
│   ├── 14-custom-keys.sh
│   ├── 15-custom-output-location.sh
│   ├── 16-date-format.sh
│   ├── 17-custom-properties.sh
│   ├── 18-extra-property.sh
│   ├── 19-escape-hatch.sh      # v3 ONLY
│   ├── 20-fail-on-no-git.sh
│   ├── 21-no-fail-on-no-git.sh
│   ├── 22-explicit-dot-git.sh
│   ├── 23-force-generation.sh
│   ├── 24-unicode-commit-message.sh
│   ├── 25-long-commit-message.sh
│   └── 26-merge-commit.sh
└── results/            # Output directory for properties files
```

## Usage

```bash
# Run all scenarios
cd /tmp/git-props-validation
./run-all.sh

# Run single scenario
./run-scenario.sh 01-basic-repo
./run-scenario.sh 05              # partial match
./run-scenario.sh dirty           # partial match

# Compare specific scenario results
./compare.sh 01-basic-repo --verbose
```

## Scenario Categories

### Git State (01-13)
- Basic repo, commits, tags (lightweight/annotated), branches
- Dirty state (staged/unstaged/untracked)
- Detached HEAD, remotes (HTTPS/SSH)

### Configuration (14-18)
- Custom keys filter
- Custom output location/name
- Date format/timezone
- Custom properties
- Extra property extension

### Special Cases (19-26)
- Scenario 19: v3 only validation (no comparison)
- Failure scenarios (failOnNoGitDirectory)
- Edge cases (unicode, long messages, merge commits)

## Comparison Logic

1. Filter `git.build.time` (always differs)
2. Sort properties alphabetically
3. Diff filtered output
4. For failure scenarios: compare exit behavior

## Verified Scenarios (sample)

| Scenario | Result |
|----------|--------|
| 01-basic-repo | PASS |
| 05-commits-after-tag | PASS |
| 08-dirty-staged | PASS |
| 19-escape-hatch | PASS (v3 only) |
| 20-fail-on-no-git | PASS |
| 24-unicode-commit-message | PASS |

## Notes

- Gradlew path: `/Users/I553838/projects/gradle-git-properties/gradlew`
- Plugin versions: v2.5.7 (Grgit) vs v3.0.0 (JGit)
- Work directories created at `/tmp/git-props-validation/work/`
- Results stored at `/tmp/git-props-validation/results/`
