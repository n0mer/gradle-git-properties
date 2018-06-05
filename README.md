## about

This repository contains packaged Gradle plugin for `git.properties` file generation.

Idea - @lievendoclo, originally published in article [Spring Boot's info endpoint, Git and Gradle - InsaneProgramming](http://www.insaneprogramming.be/article/2014/08/15/spring-boot-info-git/).

## usage

Declare this in your `build.gradle`

```groovy
plugins {
  id "com.gorylenko.gradle-git-properties" version "1.5.1"
}
```

This is enough to see git details via `info` endpoint of [spring-boot-actuator](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready).

If needed - override location of `git.properties` file like this:

```groovy
gitProperties {
    gitPropertiesDir = "${project.rootDir}/your/custom/dir"
}
```
> Please note that `spring-boot-actuator` expects `git.properties` to be available at certain location.

If needed - use `dateFormat` and `dateFormatTimeZone` to format `git.commit.time` and `git.build.time` properties (See [SimpleDateFormat](http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html) and [TimeZone](http://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html) for valid values)

```groovy
gitProperties {
    dateFormat = "yyyy-MM-dd'T'HH:mmZ"
    dateFormatTimeZone = "PST"
}
```

The `.git` directory for the project should be detected automatically, otherwise it can be specified manually using `dotGitDirectory`:

```groovy
gitProperties {
    dotGitDirectory = "${project.rootDir}/../somefolder/.git"
}
```


By default, all available git properties (which are supported by the plugin) will be generated:

```
git.branch
git.build.host
git.build.time
git.build.user.email
git.build.user.name
git.build.version
git.closest.tag.commit.count
git.closest.tag.name
git.commit.id
git.commit.id.abbrev
git.commit.id.describe
git.commit.message.full
git.commit.message.short
git.commit.time
git.commit.user.email
git.commit.user.name
git.dirty
git.remote.origin.url
git.tags
git.total.commit.count
```
You can have more fine grained control of the content of 'git.properties' using `keys`:

```groovy
gitProperties {
    keys = ['git.branch','git.commit.id','git.commit.time']
}
```

Custom properties can also be added with `customProperty` (supports both expressions and closures):

```groovy
gitProperties {
    customProperty 'greeting', 'Hello' // expression
    customProperty 'my_custom_git_id', { it.head().id } // closure, 'it' is an instance of org.ajoberstar.grgit.Grgit
    customProperty 'project_version', { project.version } // closure
}
```

The generated properties can also be accessed from project.ext by configuring `extProperty`. In the below example, `gitProps` is used as the name of the exposed model

```groovy
gitProperties {
    extProperty = 'gitProps'
}
generateGitProperties.outputs.upToDateWhen { false }

task printGitProperties(dependsOn: 'generateGitProperties') {
    doLast {
        println "git.commit.id.abbrev=" + project.ext.gitProps['git.commit.id.abbrev']
    }
}
```


> By default, the `info` endpoint exposes only `git.branch`, `git.commit.id`, and `git.commit.time` properties (even then there are more in your git.properties).
> In order to expose all available properties, set the "management.info.git.mode" property to "full" per [the Spring Boot documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html#production-ready-application-info-git), e.g. in application.properties:

> `management.info.git.mode=full`

Plugin is available from [Gradle Plugins repository](https://plugins.gradle.org/plugin/com.gorylenko.gradle-git-properties).

## notes
* Plugin requires Java 8+

## result

This is raw `JSON` from `info` endpoint (with management.info.git.mode=simple or not configured):

```json
{
  "git": {
    "commit": {
      "time": "2018-03-28T05:13:53Z",
      "id": "32ff212"
    },
    "branch": "Fix_issue_68"
  }
}
```

This is raw `JSON` from `info` endpoint (with management.info.git.mode=full):

```json
{
  "git": {
    "build": {
      "host": "myserver-1",
      "version": "0.0.1-SNAPSHOT",
      "time": "2018-03-28T05:34:35Z",
      "user": {
        "name": "First Last",
        "email": "username1@example.com"
      }
    },
    "branch": "Fix_issue_68",
    "commit": {
      "message": {
        "short": "Fix issue #68",
        "full": "Fix issue #68"
      },
      "id": {
        "describe": "v1.4.21-28-g32ff212-dirty",
        "abbrev": "32ff212",
        "full": "32ff212b9e2873fa4672f1b5dd41f67aca6e0731"
      },
      "time": "2018-03-28T05:13:53Z",
      "user": {
        "email": "username1@example.com",
        "name": "First Last"
      }
    },
    "closest": {
      "tag": {
        "name": "v1.4.21",
        "commit": {
          "count": "28"
        }
      }
    },
    "dirty": "true",
    "remote": {
      "origin": {
        "url": "git@github.com:n0mer/gradle-git-properties.git"
      }
    },
    "tags": "",
    "total": {
      "commit": {
        "count": "93"
      }
    }
  }
}
```

## license

`gradle-git-properties` is Open Source software released under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html)
