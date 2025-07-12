# Development Notes

## Docker Build and Publish

```bash
docker run --rm -v $(pwd):/app -w /app openjdk:8 ./gradlew publishPlugins -Pgradle.publish.key=your_key -Pgradle.publish.secret=your_secret
```
