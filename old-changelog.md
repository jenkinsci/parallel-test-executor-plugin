### Version 1.13 and later

See [GitHub releases](https://github.com/jenkinsci/parallel-test-executor-plugin/releases).

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
