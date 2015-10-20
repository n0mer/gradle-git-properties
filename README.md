## about

This repository contains packaged Gradle plugin for `git.properties` file generation.

Idea - @lievendoclo, originally published in article [Spring Boot's info endpoint, Git and Gradle - InsaneProgramming](http://www.insaneprogramming.be/blog/2014/08/15/spring-boot-info-git/).

## usage

Declare this in your `build.gradle`

```groovy
plugins {
  id "com.gorylenko.gradle-git-properties" version "1.4.7"
}
```

If needed - override location of `git.properties` file like this:
```groovy
gitProperties {
    gitPropertiesDir = new File("${project.rootDir}/your/custom/dir")
}
```

If project root dir is not the same git repo root dir, it can be overridden like so.
```groovy
gitProperties {
    gitRepositoryRoot = new File("${project.rootDir}/../..")
}
```

> Please note that `spring-boot-actuator` expects `git.properties` to be available at certain location.

This is enough to see git details via `info` endpoint of [spring-boot-actuator](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready).

Plugin is available from [Gradle Plugins repository](https://plugins.gradle.org/plugin/com.gorylenko.gradle-git-properties).

## result

This is raw `JSON` from `info` endpoint:

```json
{
  version: "0.0.1.BUILD-SNAPSHOT",
  id: "boot-admin-682defcca0d6",
  git: {
    branch: "master",
    commit: {
      id: "e06c7ec",
      time: "1442094398"
    }
  }
}
```

## license

`gradle-git-properties` is Open Source software released under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html)
