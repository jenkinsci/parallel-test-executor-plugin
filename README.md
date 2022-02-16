# Jenkins Parallel Test Executor Plugin

This plugin adds a tool that lets you easily execute tests in parallel.
This is achieved by having Jenkins look at the test execution time of the last run,
split tests into multiple units of roughly equal size, then execute them in parallel.

## How It Works

The tool looks at the test execution time from the last time, and divide tests into multiple units of roughly equal size. Each unit is then converted into the exclusion list (by excluding all but the tests that assigned to that unit).

This tool can be used with any test job that

1.  produce JUnit-compatible XML files
2.  accept a test-exclusion list in a file.

You are responsible for configuring the build script to honor the exclusion file. A standard technique is to write the build script to always refer to a fixed exclusion list file, and check in an empty file by that name. You can then specify that file as the "exclusion file name" in the configuration of this builder, and the builder will overwrite the empty file from SCM by the generated one.

There are two modes: one used with [Jenkins Pipeline](https://jenkins.io/doc/book/pipeline/), the other with freestyle projects. The former is more flexible and straightforward.

### Pipeline step

The `splitTests` step analyzes test results from the last successful build of this job, if any. It returns a set of roughly equal "splits", each representing one chunk of work. Typically you will use the `parallel` step to run each chunk in its own `node`, passing split information to the build tool in various ways. The demo (below) shows this in action.

### Freestyle-compatible builder

For freestyle projects, setup is more complex as you need *two* jobs, an upstream controller and a downstream workhorse. There is a build step which you add to the upstream job and on which you define the downstream job. The builder executes multiple runs of the downstream job concurrently by interleaving tests, saving configuration files to the downstream workspace, achieving the parallel test execution semantics.

You are responsible for checking **Execute concurrent builds if necessary** on the downstream job to allow the concurrent execution.

When the downstream builds all finish, the specified report directories are brought back into the upstream job's workspace, where they will be picked up by the standard JUnit test report collector.

The instructions below for configuring your build tool then apply to the downstream job.

## Configuring build tools with exclusions

### Maven

Newer version of Maven Surefire plugin supports `excludesFile` parameter. For example, the following configuration tells Maven to honor `exclusions.txt` at the root of the source tree.

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

Ant JUnit task supports the `excludesfile` attribute in its `<batchtest>` sub-element:

```xml
<batchtest fork="yes" todir="build/test-reports">
  <fileset dir="test" excludesfile="exclusions.txt">
    <include name="**/*Test*.java"/>
  </fileset>
</batchtest>
```

## Demo

The `demo` subdirectory in sources contains a demo of this plugin based on Docker. It shows both Pipeline and freestyle modes. [README](demo/README.md)

## Changelog

See [GitHub releases](https://github.com/jenkinsci/parallel-test-executor-plugin/releases)
or the [old changelog](old-changelog.md).
