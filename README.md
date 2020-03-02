# Jenkins Parallel Test Executor Plugin

This plugin adds a tool that lets you easily execute tests in parallel.This is achieved by having Jenkins look at the test execution time of the last run, split tests into multiple units of roughly equal size, then execute them in parallel.

How It Works
============

The tool looks at the test execution time from the last time, and divide tests into multiple units of roughly equal size. Each unit is then converted into the exclusion list (by excluding all but the tests that assigned to that unit).

This tool can be used with any test job that

1.  produce JUnit-compatible XML files
2.  accept a test-exclusion list in a file.

You are responsible for configuring the build script to honor the exclusion file. A standard technique is to write the build script to always refer to a fixed exclusion list file, and check in an empty file by that name. You can then specify that file as the "exclusion file name" in the configuration of this builder, and the builder will overwrite the empty file from SCM by the generated one.

There are two modes: one used with [Jenkins Pipeline](https://jenkins.io/doc/book/pipeline/), the other with freestyle projects. The former is more flexible and straightforward.

Pipeline step
-------------

The `splitTests` step analyzes test results from the last successful build of this job, if any. It returns a set of roughly equal "splits", each representing one chunk of work. Typically you will use the `parallel` step to run each chunk in its own `node`, passing split information to the build tool in various ways. The demo (below) shows this in action.

Freestyle-compatible builder
----------------------------

For freestyle projects, setup is more complex as you need *two* jobs, an upstream controller and a downstream workhorse. There is a build step which you add to the upstream job and on which you define the downstream job. The builder executes multiple runs of the downstream job concurrently by interleaving tests, saving configuration files to the downstream workspace, achieving the parallel test execution semantics.

You are responsible for checking **Execute concurrent builds if necessary** on the downstream job to allow the concurrent execution.

When the downstream builds all finish, the specified report directories are brought back into the upstream job's workspace, where they will be picked up by the standard JUnit test report collector.

The instructions below for configuring your build tool then apply to the downstream job.

Configuring build tools with exclusions
=======================================

### Maven

Newer version of Maven Surefire plugin supports **excludesFile** parameter. For example, the following configuration tells Maven to honor `exclusions.txt` at the root of the source tree.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-surefire-plugin</artifactId>
      <version>2.14</version>
      <configuration>
        <excludesFile>${project.basedir}/exclusions.txt</excludesFile>
      </configuration>
    </plugin>
  </plugins>
</build>
```

### Ant

Ant JUnit task supports the `excludesfile` attribute in its `<batchtest>` sub-element:

```xml
<batchtest fork="yes" todir="build/test-reports">
  <fileset dir="test" excludesfile="exclusions.txt">
    <include name="**/*Test*.java"/>
  </fileset>
</batchtest>
```

## Demo

The `demo` subdirectory in sources contains a demo of this plugin based on Docker. It shows both Pipeline and freestyle modes. [README](https://github.com/jenkinsci/parallel-test-executor-plugin/blob/master/demo/README.md)

## Changelog

### Version 1.12 (2018-12-10)

-   [JENKINS-53528](https://issues.jenkins-ci.org/browse/JENKINS-53528) - Parallel-test-executor can excessively split tests

### Version 1.11 (Sept 5, 2018)

-   [JENKINS-53172](https://issues.jenkins-ci.org/browse/JENKINS-53172) - Make sure the reference build isn't actively running.
-   [JENKINS-47206](https://issues.jenkins-ci.org/browse/JENKINS-47206) - Estimate split for initial build via filesystem scan.

### Version 1.10 (Jan 4, 2018)

-   [JENKINS-27395](https://issues.jenkins-ci.org/browse/JENKINS-27395) - Allow specifying a specific stage to pull tests from

### Version 1.9 (Jul 29, 2016)

-   [JENKINS-29922](https://issues.jenkins-ci.org/browse/JENKINS-29922) Adding symbols to allow for a cleaner syntax when using newer versions of Pipeline.

### Version 1.8 (Feb 03, 2016)

-   [JENKINS-29894](https://issues.jenkins-ci.org/browse/JENKINS-29894) Allow a list of includes, rather than excludes, to be specified in a split.

### Version 1.7 (May 21, 2015)

-   Fix link to wiki page ([#13](https://github.com/jenkinsci/parallel-test-executor-plugin/pull/13))

### Version 1.6 (Dec 03, 2014) (requires 1.580.1+)

-   Integration with Workflow 1.0.

### Version 1.6-beta-2 (Oct 21, 2014) (requires 1.580+)

-   Integration with Workflow 0.1-beta-5.

### Version 1.6-beta-1 (Sep 28, 2014) (requires 1.577+)

-   Added integration with Workflow: `splitTests` step

### Version 1.5 (Sep 28, 2014)

-   Compatibility with Jenkins 1.577+.

### Version 1.4 (May 6, 2014)

-   Search only successful or unstable builds for Test Results

### Version 1.3 (Feb 10, 2014)

-   Support passing parameters to test job

### Version 1.2 (Dec 14, 2013)

-   By default archive JUnit test results

### Version 1.1 (Dec 13, 2013)

-   Optionally configure the JUnit Test Result Archiver manually ([JENKINS-20825](https://issues.jenkins-ci.org/browse/JENKINS-20825))
-   Do not stop searching old builds for Test Results when a build with no test results is encountered
-   exclude both source and class files (Gradle takes class files, Ant <junit> task takes Java files)
-   Fixed a serialization issue

### Version 1.0 (Jun 2, 2013)

-   Initial release
