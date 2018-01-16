## about

This repository contains packaged Gradle plugin for `git.properties` file generation.

Idea - @lievendoclo, originally published in article [Spring Boot's info endpoint, Git and Gradle - InsaneProgramming](http://www.insaneprogramming.be/article/2014/08/15/spring-boot-info-git/).

## usage

Declare this in your `build.gradle`

```groovy
plugins {
  id "com.gorylenko.gradle-git-properties" version "1.4.20"
}
```

If needed - override location of `git.properties` file like this:
```groovy
gitProperties {
    gitPropertiesDir = new File("${project.rootDir}/your/custom/dir")
}
```

If needed - use `dateFormat` and `dateFormatTimeZone` to format `git.commit.time` value (See [SimpleDateFormat](http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html) and [TimeZone](http://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html) for values)
```groovy
gitProperties {
    dateFormat = "yyyy-MM-dd'T'HH:mmZ"
    dateFormatTimeZone = "PST"
}
```

If project root dir is not the same git repo root dir, it can be overridden like so.
```groovy
gitProperties {
    gitRepositoryRoot = new File("${project.rootDir}/../..")
}
```

> Please note that `spring-boot-actuator` expects `git.properties` to be available at certain location.

In some situations, for example when you run your build under a CI tool that checks out your code in detached mode, and HEAD no longer points to a branch name, but your CI tool
exposes the branch in an environment variable, you can explicitly set the branch name to be written into the `git.properties` file:
```groovy
gitProperties {
    gitBranchName = System.getenv('BRANCH_NAME')
}
```

This is enough to see git details via `info` endpoint of [spring-boot-actuator](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready).

You can have more fine grained control of the content of 'git.properties':
```groovy
gitProperties {
    keys = ['git.branch','git.commit.id','git.commit.time']
}
```
All available keys can be found in the [source](https://github.com/n0mer/gradle-git-properties/blob/master/src/main/groovy/com/gorylenko/GitPropertiesPlugin.groovy).

In order to see all attributes, you can set the "management.info.git.mode" property to "full" per [the Spring Boot documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html#production-ready-application-info-git), e.g. in application.properties:

`management.info.git.mode=full`

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
