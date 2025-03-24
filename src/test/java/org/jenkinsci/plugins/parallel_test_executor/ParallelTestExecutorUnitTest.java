package org.jenkinsci.plugins.parallel_test_executor;

import hudson.FilePath;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import org.apache.tools.ant.DirectoryScanner;
import org.jenkinsci.plugins.parallel_test_executor.testmode.JavaClassName;
import org.jenkinsci.plugins.parallel_test_executor.testmode.JavaParameterizedTestCaseName;
import org.jenkinsci.plugins.parallel_test_executor.testmode.JavaTestCaseName;
import org.jenkinsci.plugins.parallel_test_executor.testmode.TestClassAndCaseName;
import org.jenkinsci.plugins.parallel_test_executor.testmode.TestMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.Issue;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParallelTestExecutorUnitTest {

    @Mock
    private Run<?, ?> build;

    @Mock
    private Run<?, ?> previousBuild;

    @Mock
    private TaskListener listener;

    @Mock
    private AbstractTestResultAction action;

    private File projectRootDir;

    private DirectoryScanner scanner;

    @BeforeEach
    void setUp() throws Exception {
        when(build.getPreviousBuild()).thenReturn((Run) previousBuild);
        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(previousBuild.getUrl()).thenReturn("job/some-project/1");
        when(previousBuild.getDisplayName()).thenReturn("#1");
        when(listener.getLogger()).thenReturn(System.err);
        when(previousBuild.getAction(AbstractTestResultAction.class)).thenReturn(action);
    }

    @BeforeEach
    void findProjectRoot(TestInfo info) {
        URL url = getClass().getResource(getClass().getSimpleName() + "/" + info.getTestMethod().orElseThrow().getName());
        assumeTrue(url != null, "The test resource for " + info.getTestMethod().orElseThrow().getName() + " exist");
        try {
            projectRootDir = new File(url.toURI());
        } catch (URISyntaxException e) {
            projectRootDir = new File(url.getPath());
        }
        scanner = new DirectoryScanner();
        scanner.setBasedir(projectRootDir);
        scanner.scan();
    }

    @Test
    void findTestSplits() throws Exception {
        checkTestSplits(new CountDrivenParallelism(5), 5, null);
        checkTestSplits(new CountDrivenParallelism(5), 5, new JavaClassName());
        // Only 5 classes
        checkTestSplits(new CountDrivenParallelism(10), 5, new JavaClassName());
        // Splitting by test cases we can parallelize more!
        checkTestSplits(new CountDrivenParallelism(10), 10, new JavaTestCaseName());
    }

    @Test
    void findTestDuplicates() throws Exception {
        checkTestSplits(new CountDrivenParallelism(10), 10, new JavaTestCaseName());
    }

    @Test
    void findTestCaseTimeSplitsExclusion() throws Exception {
        TimeDrivenParallelism parallelism = new TimeDrivenParallelism(2);
        checkTestSplits(parallelism, 5, new TestClassAndCaseName());
    }

    public void checkTestSplits(Parallelism parallelism, int expectedSplitSize, TestMode testMode) throws Exception {
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);

        List<InclusionExclusionPattern> splits = Splitter.findTestSplits(parallelism, testMode, build, listener, false, null, null);
        assertEquals(expectedSplitSize, splits.size());
        for (InclusionExclusionPattern split : splits) {
            assertFalse(split.isIncludes());
        }
    }

    @Test
    void testWeDoNotCreateMoreSplitsThanThereAreTests() throws Exception {
        // The test report only has 2 classes, so we should only split into 2 test executors
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);

        CountDrivenParallelism parallelism = new CountDrivenParallelism(5);
        List<InclusionExclusionPattern> splits = Splitter.findTestSplits(parallelism, null, build, listener, false, null, null);
        assertEquals(2, splits.size());
        for (InclusionExclusionPattern split : splits) {
            assertFalse(split.isIncludes());
        }
    }

    @Test
    void findTestCasesWithParameters() throws Exception {
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);
        CountDrivenParallelism parallelism = new CountDrivenParallelism(3);
        List<InclusionExclusionPattern> splits = Splitter.findTestSplits(parallelism, new JavaTestCaseName(), build, listener, false, null, null);
        assertEquals(3, splits.size());
        var allSplits = splits.stream().flatMap(s -> s.getList().stream()).collect(Collectors.toSet());
        assertThat(allSplits, hasSize(20));
        assertThat(allSplits, hasItem("org.jenkinsci.plugins.parallel_test_executor.Test1#testCase"));
    }

    @Test
    void findTestCasesWithParametersIncluded() throws Exception {
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);
        CountDrivenParallelism parallelism = new CountDrivenParallelism(3);
        List<InclusionExclusionPattern> splits = Splitter.findTestSplits(parallelism, new JavaParameterizedTestCaseName(), build, listener, false, null, null);
        assertEquals(3, splits.size());
        var allSplits = splits.stream().flatMap(s -> s.getList().stream()).collect(Collectors.toSet());
        assertThat(allSplits, hasSize(22));
        assertThat(allSplits, hasItem("org.jenkinsci.plugins.parallel_test_executor.Test1#testCase[param1]"));
    }

    @Test
    void findTestSplitsInclusions() throws Exception {
        CountDrivenParallelism parallelism = new CountDrivenParallelism(5);
        checkTestSplitsInclusions(parallelism, 5, null);
    }

    @Test
    void findTestCaseTimeSplitsInclusion() throws Exception {
        TimeDrivenParallelism parallelism = new TimeDrivenParallelism(2);
        checkTestSplitsInclusions(parallelism, 5, new TestClassAndCaseName());
    }

    private void checkTestSplitsInclusions(Parallelism parallelism, int expectedSplitSize, TestMode testMode) throws Exception {
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);

        List<InclusionExclusionPattern> splits = Splitter.findTestSplits(parallelism, testMode, build, listener, true, null, null);
        assertEquals(expectedSplitSize, splits.size());
        List<String> exclusions = new ArrayList<>(splits.get(0).getList());
        List<String> inclusions = new ArrayList<>();
        for (int i = 0; i < splits.size(); i++) {
            InclusionExclusionPattern split = splits.get(i);
            assertEquals(i != 0, split.isIncludes());
            if (split.isIncludes()) {
                inclusions.addAll(split.getList());
            }
        }
        Collections.sort(exclusions);
        Collections.sort(inclusions);
        assertEquals(inclusions, exclusions, "exclusions set should contain all elements included by inclusions set");
    }

    @Issue("JENKINS-47206")
    @Test
    void findTestInJavaProjectDirectory() throws InterruptedException {
        CountDrivenParallelism parallelism = new CountDrivenParallelism(5);
        List<InclusionExclusionPattern> splits = Splitter.findTestSplits(parallelism, null, build, listener, true, null, new FilePath(scanner.getBasedir()));
        assertEquals(5, splits.size());
    }

    @Issue("JENKINS-47206")
    @Test
    void findTestOfJavaProjectDirectoryInWorkspace() throws InterruptedException {
        CountDrivenParallelism parallelism = new CountDrivenParallelism(5);
        Map<String, TestEntity> data = TestMode.getDefault().estimate(new FilePath(scanner.getBasedir()), listener);
        Set<String> expectedTests = new HashSet<>();
        expectedTests.add("FirstTest");
        expectedTests.add("SecondTest");

        expectedTests.add("somepackage/ThirdTest");
        expectedTests.add("ThirdTest");
        expectedTests.add("FourthTest");
        expectedTests.add("FifthTest");
        assertEquals(expectedTests, data.keySet(), "Result does not contains expected tests.");
        List<InclusionExclusionPattern> splits = Splitter.findTestSplits(parallelism, null, build, listener, true, null, new FilePath(scanner.getBasedir()));
        assertEquals(5, splits.size());
    }

    @Test
    void previousBuildIsOngoing() throws IOException {
        Job project = mock(Job.class);
        Run previousPreviousBuild = mock(Run.class);
        when(previousBuild.getResult()).thenReturn(null);
        when(previousBuild.getPreviousBuild()).thenReturn(previousPreviousBuild);
        when(previousPreviousBuild.getParent()).thenReturn(project);
        when(previousPreviousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(previousPreviousBuild.getAction(eq(AbstractTestResultAction.class))).thenReturn(action);
        when(previousPreviousBuild.getUrl()).thenReturn("job/some-project/1");
        when(previousPreviousBuild.getDisplayName()).thenReturn("#1");
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);

        assertNotNull(Splitter.getTestResult(project, previousBuild, listener));
    }
}
