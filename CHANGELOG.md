# Changelog

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
