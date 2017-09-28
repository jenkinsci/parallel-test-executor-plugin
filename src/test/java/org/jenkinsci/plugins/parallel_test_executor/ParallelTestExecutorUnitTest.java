package org.jenkinsci.plugins.parallel_test_executor;

import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import org.apache.tools.ant.DirectoryScanner;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

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
        CountDrivenParallelism parallelism = new CountDrivenParallelism(5);
        checkTestSplits(parallelism, 5, TestMode.JAVA);
    }
    
    @Test
    public void findTestCaseTimeSplitsExclusion() throws Exception {
        TimeDrivenParallelism parallelism = new TimeDrivenParallelism(2);
        checkTestSplits(parallelism, 5, TestMode.CLASSANDTESTCASENAME);
    }

    public void checkTestSplits(Parallelism parallelism, int expectedSplitSize, TestMode testMode) throws Exception {
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);

        List<InclusionExclusionPattern> splits = ParallelTestExecutor.findTestSplits(parallelism, build, listener, false, testMode);
        assertEquals(expectedSplitSize, splits.size());
        for (InclusionExclusionPattern split : splits) {
            assertFalse(split.isIncludes());
        }
    }

    @Test
    public void findTestSplitsInclusions() throws Exception {
        CountDrivenParallelism parallelism = new CountDrivenParallelism(5);
        checkTestSplitsInclusions(parallelism, 5, null);
    }
    
    @Test
    public void findTestCaseTimeSplitsInclusion() throws Exception {
        TimeDrivenParallelism parallelism = new TimeDrivenParallelism(2);
        checkTestSplitsInclusions(parallelism, 5, TestMode.CLASSANDTESTCASENAME);
    }
    
    private void checkTestSplitsInclusions(Parallelism parallelism, int expectedSplitSize, TestMode testMode) throws Exception {
        TestResult testResult = new TestResult(0L, scanner, false);
        testResult.tally();
        when(action.getResult()).thenReturn(testResult);

        List<InclusionExclusionPattern> splits = ParallelTestExecutor.findTestSplits(parallelism, build, listener, true, testMode);
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
        assertEquals("exclusions set should contain all elements included by inclusions set", inclusions, exclusions);
    }
}
