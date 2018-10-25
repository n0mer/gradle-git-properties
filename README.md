## about

This Gradle plugin can be used for generating `git.properties` file generation for Git-based projects (similar to maven git commit id plugin). It can be used for (but not limited to) Spring Boot apps.
Plugin is available from [Gradle Plugins repository](https://plugins.gradle.org/plugin/com.gorylenko.gradle-git-properties).

Idea - @lievendoclo, originally published in article [Spring Boot's info endpoint, Git and Gradle - InsaneProgramming](http://www.insaneprogramming.be/article/2014/08/15/spring-boot-info-git/).

[![Build Status](https://travis-ci.org/n0mer/gradle-git-properties.svg?branch=master)](https://travis-ci.org/n0mer/gradle-git-properties)

## notes
* Plugin requires Java 8+

## usage

Declare this in your `build.gradle`

```groovy
plugins {
  id "com.gorylenko.gradle-git-properties" version "1.5.2"
}
```

Or use 2.0.0-beta1 (beta version, with Build Cache support, see https://docs.gradle.org/current/userguide/build_cache.html)

```groovy
plugins {
  id "com.gorylenko.gradle-git-properties" version "2.0.0-beta1"
}
```

A git.properties file will be generated when building Java-based projects (the plugin will configure any existing `classes` task to depend on `generateGitProperties` task - which is responsible for generated git.properties file). For non-Java projects, `generateGitProperties` task must be executed explicitly to generate git.properties file. The git repository for the project will be used.

Spring Boot specific info: This is enough to see git details via `info` endpoint of [spring-boot-actuator](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready).

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

If needed - use `branch` to override git branch name (when it cannot be detected correctly from environment variables/working directory)

```groovy
gitProperties {
    branch = System.getenv('GIT_BRANCH')
}
```


By default, all git properties which are supported by the plugin will be generated:

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

Custom properties can be added with `customProperty` (supports both expressions and closures):

```groovy
gitProperties {
    customProperty 'greeting', 'Hello' // expression
    customProperty 'my_custom_git_id', { it.head().id } // closure, 'it' is an instance of org.ajoberstar.grgit.Grgit
    customProperty 'project_version', { project.version } // closure
}
```


> Spring Boot specific info: By default, the `info` endpoint exposes only `git.branch`, `git.commit.id`, and `git.commit.time` properties (even then there are more in your git.properties).
> In order to expose all available properties, set the "management.info.git.mode" property to "full" per [the Spring Boot documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html#production-ready-application-info-git), e.g. in application.properties:

> `management.info.git.mode=full`


The `.git` directory for the project should be detected automatically, otherwise it can be specified manually using `dotGitDirectory`:

```groovy
gitProperties {
    dotGitDirectory = "${project.rootDir}/../somefolder/.git"
}
```


## result from `info` endpoint (if used with Spring Boot apps)

When using with Spring Boot: This is raw `JSON` from `info` endpoint (with management.info.git.mode=simple or not configured):

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

### other usages

Although this plugin is used mostly for generating git.properties files, the generated git properties could also be used for other purposes (by configuring `extProperty` to keep generated properties and accessing the properties from `project.ext`). Note that the git.properties file is always generated and currently these is no option to disable it. Also please make sure that the `generateGitProperties` task is executed before accessing the generated properties.

In the below example, `printGitProperties` will print `git.commit.id.abbrev` property when it is executed:

```groovy
gitProperties {
    extProperty = 'gitProps' // git properties will be put in a map at project.ext.gitProps
}
generateGitProperties.outputs.upToDateWhen { false } // make sure the generateGitProperties task always executes (even when git.properties is not changed)

task printGitProperties(dependsOn: 'generateGitProperties') { // make sure generateGitProperties task to execute before accessing generated properties
    doLast {
        println "git.commit.id.abbrev=" + project.ext.gitProps['git.commit.id.abbrev']
    }
}
```

Below is another example about using generated properties for MANIFEST.MF of a Spring Boot webapp (similar can be done for non Spring apps). Note the usage of GString lazy evaluation to delay evaluating `project.ext.gitProps['git.commit.id.abbrev']` until MANIFEST.MF is created. Because `generateGitProperties` task will always execute automatically before any `classes` task (in Java projects), no `dependOn` is needed for `bootJar` task.

```groovy
gitProperties {
    extProperty = 'gitProps' // git properties will be put in a map at project.ext.gitProps
}
generateGitProperties.outputs.upToDateWhen { false } // make sure the generateGitProperties task always executes (even when git.properties is not changed)

bootJar {
  manifest {
    attributes(
        'Build-Revision': "${-> project.ext.gitProps['git.commit.id.abbrev']}"  // Use GString lazy evaluation to delay until git properties are populated
    )
  }
}
```

## license

`gradle-git-properties` is Open Source software released under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html)
