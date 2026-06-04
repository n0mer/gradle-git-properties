# Migration Guide

## 3.x to 4.0

### Output Directory Changed (Breaking)

`generateGitProperties` now writes to `build/generated/resources/git/` instead of `build/resources/main/`.

**The location of `git.properties` inside your JAR is unchanged** — `processResources` still copies it there.

**Why:** Writing into `build/resources/main/` caused Gradle's stale-output detection to silently delete `git.properties` on alternating `clean build` runs when the build cache was enabled (Gradle 8.6+).

**Migration by case:**

| Situation | Action |
|-----------|--------|
| Default config (no `gitPropertiesDir` / `gitPropertiesResourceDir`) | None — plugin auto-wires everything |
| Referencing `build/resources/main/git.properties` after `assemble` | Still valid — no change needed |
| Referencing `build/resources/main/git.properties` before `processResources` | Update path to `build/generated/resources/git/git.properties` |
| `gitPropertiesResourceDir` set explicitly | Unchanged |
| `gitPropertiesDir` set explicitly | Unchanged, but deprecated — migrate to `gitPropertiesResourceDir` |
| `gitPropertiesDir` set to path inside `build/resources/main/` | Use `gitPropertiesName` with relative path instead |
| Non-Java / Android projects | Unaffected — sourceSets wiring requires the `java` plugin |

### `gitPropertiesDir` Deprecated

Use `gitPropertiesResourceDir` instead. `gitPropertiesDir` still works but no longer triggers classpath wiring, and will be removed in a future version.

```groovy
// Before
gitProperties {
    gitPropertiesDir = file('custom-git-props')
}

// After
gitProperties {
    gitPropertiesResourceDir = file('custom-git-props')
}
```

### Custom JAR Subpath (issue #306)

Some users set `gitPropertiesDir` to a path inside `build/resources/main/` in order to control where the file appeared inside the JAR. For example:

```groovy
// Before (v3) — caused silent build cache corruption
gitProperties {
    gitPropertiesDir = file("${buildDir}/resources/main/discord4j/common")
}
// Result in JAR: discord4j/common/git.properties
```

The v4.0 migration guide said to rename `gitPropertiesDir` to `gitPropertiesResourceDir`. However, pointing `gitPropertiesResourceDir` at a path inside `build/resources/main/` recreates the overlapping-output problem that v4.0 was designed to fix: `generateGitProperties` and `processResources` would both own the same output directory, causing stale-output corruption on cached builds.

As of v4.0.1, use `gitPropertiesName` with a relative path instead:

```groovy
// After (v4.0.1) — correct
gitProperties {
    gitPropertiesName = "discord4j/common/git.properties"
}
// Result in JAR: discord4j/common/git.properties
```

`gitPropertiesName` separates the JAR subpath concern from the write-location concern. The file is still written to `build/generated/resources/git/` (under the relative path), and `processResources` copies it into the JAR at the correct subpath.

Invalid values (`null`, paths starting with `/`, paths containing `..` segments) are rejected at configuration time with the error: `gitPropertiesName must be a relative path that stays under gitPropertiesResourceDir`.

## 2.x to 3.0

## Breaking Changes

### Java 17 Required

Version 3.0 requires Java 17 or higher. This is due to the upgrade to JGit 7.x which has this requirement.

**Before:** Java 8+
**After:** Java 17+

### Custom Property API Changes

The custom property closure parameter type changed from Grgit to `GitFacade`.

**Groovy - No changes required.** Closures work dynamically.

**Kotlin - Import path changed:**

```kotlin
// Before (2.x)
import gradlegitproperties.org.ajoberstar.grgit.Grgit
customProperty("my_id", KotlinClosure1<Grgit, String>({ head().id }))

// After (3.0)
import com.gorylenko.jgit.GitFacade
customProperty("my_id", KotlinClosure1<GitFacade, String>({ head().id }))
```

### API Compatibility

Most common operations remain compatible:

| Operation | 2.x (Grgit) | 3.0 (GitFacade) | Status |
|-----------|-------------|-----------------|--------|
| `head().id` | Yes | Yes | Compatible |
| `head().abbreviatedId` | Yes | Yes | Compatible |
| `head().author.name` | Yes | Yes | Compatible |
| `head().author.email` | Yes | Yes | Compatible |
| `head().shortMessage` | Yes | Yes | Compatible |
| `head().fullMessage` | Yes | Yes | Compatible |
| `describe()` | Yes | Yes | Compatible |
| `describe(tags: true)` | Yes | Yes | Compatible |
| `status().clean` | `isClean()` | `.clean` | Minor change |
| `log()` | Yes | Yes | Compatible |

### Escape Hatch for Advanced Usage

If you used Grgit-specific APIs not covered by GitFacade, use the escape hatch:

```groovy
gitProperties {
    // Access raw JGit Repository
    customProperty 'custom', { it.jgit.refDatabase.refs.size() }
    
    // Access JGit Git command interface
    customProperty 'custom2', {
        def git = it.jgitCommands
        try {
            return git.stashList().call().size()
        } finally {
            git.close()
        }
    }
}
```

## New Features in 3.0

### Git Worktree Support

The plugin now correctly detects branch names and other metadata when running inside a git worktree.

### Improved Performance

Direct JGit integration removes the Grgit abstraction layer, improving performance for large repositories.
