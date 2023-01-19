package org.jenkinsci.plugins.parallel_test_executor;

import hudson.FilePath;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import java.io.IOException;
import org.apache.tools.ant.DirectoryScanner;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeThat;
import org.jvnet.hudson.test.Issue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ParallelTestExecutorUnitTest {

    ParallelTestExecutor instance;

    @Mock Run<?, ?> build;

    @Mock Run<?, ?> previousBuild;

    @Mock TaskListener listener;

    @Mock AbstractTestResultAction action;

    @Rule public TestName name = new TestName();

    File projectRootDir;

    DirectoryScanner scanner;


    @Before
    public void setUp() throws Exception {
        when(build.getPreviousBuild()).thenReturn((Run)previousBuild);
        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(previousBuild.getUrl()).thenReturn("job/some-project/1");
        when(previousBuild.getDisplayName()).thenReturn("#1");
        when(listener.getLogger()).thenReturn(System.err);
        when(previousBuild.getAction(eq(AbstractTestResultAction.class))).thenReturn(action);
    }

    @Before
    public void findProjectRoot() throws Exception {
        URL url = getClass().getResource(getClass().getSimpleName() + "/" + this.name.getMethodName());
        assumeThat("The test resource for " + this.name.getMethodName() + " exist", url, Matchers.notNullValue());
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
    public void findTestSplits() throws Exception {
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);

        CountDrivenParallelism parallelism = new CountDrivenParallelism(5);
        List<InclusionExclusionPattern> splits = ParallelTestExecutor.findTestSplits(parallelism, build, listener, false, null, null, false);
        assertEquals(5, splits.size());
        for (InclusionExclusionPattern split : splits) {
            assertFalse(split.isIncludes());
        }
    }

    @Test
    public void testWeDoNotCreateMoreSplitsThanThereAreTests() throws Exception {
        // The test report only has 2 classes, so we should only split into 2 test executors
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);

        CountDrivenParallelism parallelism = new CountDrivenParallelism(5);
        List<InclusionExclusionPattern> splits = ParallelTestExecutor.findTestSplits(parallelism, build, listener, false, null, null, false);
        assertEquals(2, splits.size());
        for (InclusionExclusionPattern split : splits) {
            assertFalse(split.isIncludes());
        }
    }

    @Test
    public void findTestSplitsInclusions() throws Exception {
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);

        CountDrivenParallelism parallelism = new CountDrivenParallelism(5);
        List<InclusionExclusionPattern> splits = ParallelTestExecutor.findTestSplits(parallelism, build, listener, true, null, null, false);
        assertEquals(5, splits.size());
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
        assertEquals("exclusions set should contain all elements included by inclusions set", inclusions, exclusions);
    }

    @Issue("JENKINS-47206")
    @Test
    public void findTestInJavaProjectDirectory(){
        CountDrivenParallelism parallelism = new CountDrivenParallelism(5);
        List<InclusionExclusionPattern> splits = ParallelTestExecutor.findTestSplits(parallelism, build, listener, true, null, new FilePath(scanner.getBasedir()), true);
        assertEquals(5, splits.size());
    }

    @Issue("JENKINS-47206")
    @Test
    public void findTestOfJavaProjectDirectoryInWorkspace(){
        CountDrivenParallelism parallelism = new CountDrivenParallelism(5);
        Map<String,TestClass> data = ParallelTestExecutor.findTestResultsInDirectory(build, listener, new FilePath(scanner.getBasedir()));
        Set<String> expectedTests = new HashSet<>();
        expectedTests.add("FirstTest");
        expectedTests.add("SecondTest");

        expectedTests.add("somepackage/ThirdTest");
        expectedTests.add("ThirdTest");
        expectedTests.add("FourthTest");
        expectedTests.add("FifthTest");
        assertEquals("Result does not contains expected tests.", expectedTests, data.keySet());
        List<InclusionExclusionPattern> splits = ParallelTestExecutor.findTestSplits(parallelism, build, listener, true, null, new FilePath(scanner.getBasedir()), true);
        assertEquals(5, splits.size());
    }

    @Test
    public void previousBuildIsOngoing() throws IOException {
        Job project = mock(Job.class);
        Run previousPreviousBuild = mock(Run.class);
        when(previousBuild.getParent()).thenReturn(project);
        when(previousBuild.getResult()).thenReturn(null);
        when(previousBuild.isBuilding()).thenReturn(true);
        when(previousBuild.getPreviousBuild()).thenReturn(previousPreviousBuild);
        when(previousBuild.getAction(eq(AbstractTestResultAction.class))).thenReturn(null);
        when(previousPreviousBuild.getParent()).thenReturn(project);
        when(previousPreviousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(previousPreviousBuild.getAction(eq(AbstractTestResultAction.class))).thenReturn(action);
        when(previousPreviousBuild.getUrl()).thenReturn("job/some-project/1");
        when(previousPreviousBuild.getDisplayName()).thenReturn("#1");
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);

        assertNotNull(ParallelTestExecutor.getTestResult(project, previousBuild, listener));
    }
}
