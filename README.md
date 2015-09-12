## about

This repository contains packaged Gradle plugin for `git.properties` file generation.

Idea - @lievendoclo, originally published in article [Spring Boot's info endpoint, Git and Gradle - InsaneProgramming](http://www.insaneprogramming.be/blog/2014/08/15/spring-boot-info-git/).

## usage

Declare this in your `build.gradle`

```groovy
apply plugin: `com.gorylenko.gradle-git-properties'
```

and you're done.

This is enough to see git details via `info` endpoint of [spring-boot-actuator](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready). 