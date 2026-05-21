# Migration Guide: 2.x to 3.0

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
