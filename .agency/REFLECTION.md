# Reflection: Grgit to JGit 7.x Migration

**Date:** 2026-05-21
**Sprint:** Single session

---

## What Worked Well

1. **Incremental TDD approach** — Each task was small enough to complete in one RED→GREEN cycle
2. **Dual-path strategy** — Keeping Grgit methods during transition allowed incremental validation
3. **GitFacade design** — Clean separation between facade (public API) and internal JGit usage
4. **Test infrastructure first** — JGitTestHelper made all subsequent testing easier
5. **Phase-based task grouping** — Batching 4-5 tasks per implementer invocation balanced context vs overhead

## What Caused Issues

1. **Worktree complexity** — The `.git` file format (pointer to `.git/worktrees/<name>`) required careful handling in RepositoryFactory
2. **Submodule edge case** — Plugin passes submodule gitdir directly, needed special detection
3. **Test method ambiguity** — Groovy couldn't resolve between `doCall(Grgit)` and `doCall(GitFacade)` when passing null — required explicit casts
4. **Tag handling** — JGit `describe()` only sees annotated tags by default; needed `tags: true` for lightweight

## Patterns to Repeat

- **Facade pattern for library migration** — Provides backward-compatible API surface
- **Escape hatch for power users** — `jgit`/`jgitCommands` allow advanced usage without bloating facade
- **Test helper investment** — JGitTestHelper paid for itself many times over
- **Phase batching** — 4-5 related tasks per agent invocation is the sweet spot

## Patterns to Avoid

- **Don't assume file structure** — `.git` can be a directory OR a file (worktree), or the plugin might pass a gitdir directly (submodule)
- **Don't skip edge case tests** — Shallow clone, detached HEAD, submodules all had surprises

## Learnings for Future

1. When migrating from a wrapper library (Grgit) to underlying library (JGit), create a thin facade that mimics the wrapper's API
2. JGit worktree handling: always use `RepositoryBuilder.setGitDir()` with proper commondir detection
3. Java version bumps (8→17) should be done last to avoid masking other issues

---

## Metrics

| Metric | Value |
|--------|-------|
| Total tasks | 45 |
| New files created | 15 (9 facade + 6 test) |
| Files modified | 30+ |
| Final test count | 280+ |
| Test failures | 0 |
| Duration | ~4 hours agent time |
