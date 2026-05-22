# Development Notes

## Docker Build and Publish

```bash
docker run --rm -v $(pwd):/gradle-git-properties -v ~/.gradle:/root/.gradle -w /gradle-git-properties eclipse-temurin:17 ./gradlew clean publishPlugins
```

**Note:** Credentials are read from `~/.gradle/gradle.properties` (mounted into container). Add these to your local file:
```properties
gradle.publish.key=your_key
gradle.publish.secret=your_secret
```

**Note:** The working directory must match the project name (`gradle-git-properties`) to ensure the correct artifactId is published to Maven Central. Using a generic name like `/app` will cause Gradle to publish with the wrong artifactId (see issue #255).
