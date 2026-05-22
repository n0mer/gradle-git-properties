# Reflection: Issue #234 - Configurable Commit ID Abbreviation Length

## What Worked Well

1. **TDD approach** - Writing tests first (RED) caught design issues early and ensured each component worked before integration.

2. **Thorough research phase** - Exploring Git standards, Maven plugin patterns, and JGit API before design prevented rework.

3. **Following existing patterns** - Using `dateFormat` as a template for threading config through layers made the implementation predictable.

4. **Small, focused tasks** - Breaking into 8 tasks with clear dependencies allowed steady progress and easy verification.

5. **Default parameter in Groovy** - Using `int commitIdAbbrevLength = 7` in `generate()` signature maintained backward compatibility with existing callers.

## What Could Be Improved

1. **Initial plan missed TDD format** - Had to restructure plan mid-way to show RED→GREEN steps explicitly. Should have followed Agency skill's TDD requirement from the start.

2. **Documentation task was forgotten** - README update was added after user feedback. Should include docs in initial planning.

3. **Worktree setup complexity** - The bare repo worktree structure caused test execution issues (`generateGitProperties` task failed on plugin's own repo). Had to use `-x generateGitProperties` workaround.

## Patterns to Repeat

- **Research competitors first** - Checking Maven plugin's `abbrevLength` implementation informed our property naming and validation range.
- **Validate at config time** - Throwing `IllegalArgumentException` in setter provides clear, early feedback.
- **Keep backward compat** - Default parameter values and keeping `GitCommit.abbreviatedId` unchanged preserved existing behavior.

## Patterns to Avoid

- **Don't assume task list is complete** - Always review for docs, changelog, migration guide.
- **Don't skip TDD format in plans** - Explicitly show test-first steps.

## Technical Learnings

1. **JGit abbreviation range is 2-40** - Not 4-40 like Git CLI enforces. We followed JGit's constraint.

2. **Groovy default parameters** - `method(int x = 7)` creates overloaded methods, maintaining binary compatibility.

3. **GitCommit vs GitFacade** - `GitCommit.abbreviatedId` is a cached value (always 7); `facade.getAbbreviatedId(n)` is computed. Both are needed for different use cases.

## Metrics

- **Tasks:** 8 total, 0 blocked
- **Test count:** ~15 new tests added
- **Files changed:** 11 (5 main, 5 test, 1 doc)
- **Execution time:** Single session
