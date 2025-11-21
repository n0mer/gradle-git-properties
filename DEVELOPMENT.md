# Development Notes

## Docker Build and Publish

```bash
docker run --rm -v $(pwd):/gradle-git-properties -w /gradle-git-properties openjdk:8 ./gradlew publishPlugins -Pgradle.publish.key=your_key -Pgradle.publish.secret=your_secret
```

**Note:** The working directory must match the project name (`gradle-git-properties`) to ensure the correct artifactId is published to Maven Central. Using a generic name like `/app` will cause Gradle to publish with the wrong artifactId (see issue #255).
