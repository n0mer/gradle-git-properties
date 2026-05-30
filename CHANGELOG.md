# Changelog

## 4.0.0

### Breaking Changes

- **Default output directory changed** - `generateGitProperties` now writes to `build/generated/resources/git/` instead of `build/resources/main/`. The file still ends up at the root of the JAR — `processResources` copies it there.

### Changed

- **`processResources` auto-wired** - When the `java` plugin is applied, `processResources` automatically depends on `generateGitProperties` and `build/generated/resources/git/` is added as a resource source directory.
- **`gitPropertiesDir` deprecated** - Use `gitPropertiesResourceDir` instead. The deprecated property no longer triggers classpath wiring.

### Fixed

- **Overlapping outputs bug** ([#212](https://github.com/n0mer/gradle-git-properties/issues/212), [#233](https://github.com/n0mer/gradle-git-properties/issues/233)) - `git.properties` was silently deleted on alternating `clean build` runs when the Gradle build cache was enabled. Fixed by separating the generate output directory from the `processResources` input directory.

See [README.md](README.md#migration-guide) for upgrade instructions.

## 3.0.0

### Breaking Changes

- **Java 17 required** - Minimum Java version raised from 8 to 17 (JGit 7.x requirement)
- **Kotlin DSL** - Import path changed from `gradlegitproperties.org.ajoberstar.grgit.Grgit` to `com.gorylenko.jgit.GitFacade`

### Changed

- **Migrated from Grgit to JGit 7.x** - Direct JGit integration replaces the Grgit library
- **New GitFacade API** - Custom property closures receive `GitFacade` instead of `Grgit` (API remains compatible for common operations)

### Added

- **Git worktree support** - Plugin correctly detects branch names and metadata in git worktrees
- **Escape hatch API** - `it.jgit` and `it.jgitCommands` for advanced JGit operations

### Removed

- **Grgit dependency** - No longer bundled; use escape hatch for advanced git operations

See [MIGRATION.md](MIGRATION.md) for upgrade instructions.

## 2.5.7

- Documentation improvements

## 2.5.6 and earlier

See [GitHub Releases](https://github.com/n0mer/gradle-git-properties/releases) for earlier versions.
