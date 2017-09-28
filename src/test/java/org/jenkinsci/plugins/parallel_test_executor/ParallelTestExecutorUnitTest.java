package org.jenkinsci.plugins.parallel_test_executor;

import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import org.apache.tools.ant.DirectoryScanner;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ParallelTestExecutorUnitTest {

    ParallelTestExecutor instance;

    @Mock Run<?, ?> build;

    @Mock Run<?, ?> previousBuild;

    @Mock Run<?, ?> newBuild;

    @Mock TaskListener listener;

    @Mock AbstractTestResultAction action;

    @Mock AbstractTestResultAction action2;

    @Rule public TestName name = new TestName();

    File projectRootDir;

    DirectoryScanner scanner;


    private void simpleSetUp() throws Exception {
        when(build.getPreviousBuild()).thenReturn((Run)previousBuild);
        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(listener.getLogger()).thenReturn(System.err);
        when(previousBuild.getAction(eq(AbstractTestResultAction.class))).thenReturn(action);
    }

    @Before
    public void findProjectRoot() throws Exception {
        URL url = getClass().getResource(getClass().getSimpleName() + "/" + this.name.getMethodName());
        assertThat("The test resource for " + this.name.getMethodName() + " exists", url, Matchers.notNullValue());
        try {
            projectRootDir = new File(url.toURI());
        } catch (URISyntaxException e) {
            projectRootDir = new File(url.getPath());
        }
        scanner = new DirectoryScanner();
        scanner.setBasedir(projectRootDir);
        scanner.scan();
        assertThat(scanner.getIncludedFiles(), Matchers.not(Matchers.emptyArray()));
    }

    @Test
    public void findTestSplits() throws Exception {
        simpleSetUp();
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);

        CountDrivenParallelism parallelism = new CountDrivenParallelism(5);
        List<InclusionExclusionPattern> splits = ParallelTestExecutor.findTestSplits(parallelism, build, listener, false);
        assertEquals(5, splits.size());
        for (InclusionExclusionPattern split : splits) {
            assertFalse(split.isIncludes());
        }
    }

    @Test
    public void findSplitsAfterAbort() throws Exception {
        // No good build to check.
        when(previousBuild.getNumber()).thenReturn(1);
        when(build.getNumber()).thenReturn(2);
        when(build.getPreviousBuild()).thenReturn((Run)previousBuild);
        when(previousBuild.getResult()).thenReturn(Result.ABORTED);
        when(listener.getLogger()).thenReturn(System.err);
        when(previousBuild.getAction(eq(AbstractTestResultAction.class))).thenReturn(action);
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);
        List<InclusionExclusionPattern> splits = ParallelTestExecutor.findTestSplits(new CountDrivenParallelism(5), build, listener, false);
        assertEquals(5, splits.size());
        assertThat(splits.get(0).getList(), Matchers.not(Matchers.empty()));
        // Ignore intermediate bad builds.
        when(newBuild.getNumber()).thenReturn(3);
        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(previousBuild.getAction(eq(AbstractTestResultAction.class))).thenReturn(action);
        when(build.getResult()).thenReturn(Result.ABORTED);
        TestResult empty = new TestResult();
        empty.tally();
        when(action2.getResult()).thenReturn(empty);
        when(build.getAction(eq(AbstractTestResultAction.class))).thenReturn(action2);
        when(newBuild.getPreviousBuild()).thenReturn((Run)build);
        splits = ParallelTestExecutor.findTestSplits(new CountDrivenParallelism(5), newBuild, listener, false);
        assertEquals(5, splits.size());
        assertThat(splits.get(0).getList(), Matchers.not(Matchers.empty()));
    }

    @Test
    public void findTestSplitsInclusions() throws Exception {
        simpleSetUp();
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);

        CountDrivenParallelism parallelism = new CountDrivenParallelism(5);
        List<InclusionExclusionPattern> splits = ParallelTestExecutor.findTestSplits(parallelism, build, listener, true);
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
}
