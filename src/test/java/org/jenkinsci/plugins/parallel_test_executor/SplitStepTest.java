package org.jenkinsci.plugins.parallel_test_executor;

import hudson.FilePath;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.InputStream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link SplitStep}.
 */
public class SplitStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();
    private WorkflowJob job;

    @Before
    public void setUp() throws Exception {
        j.jenkins.setCrumbIssuer(null);
        job = j.jenkins.createProject(WorkflowJob.class, "WFJob");
        FilePath dir = j.jenkins.getRootPath().child("workspace").child("WFJob");
        dir.mkdirs();

        dir.child("report-Test11.xml").copyFrom(getTest("report-Test1.xml"));
        dir.child("report-Test12.xml").copyFrom(getTest("report-Test2.xml"));
        dir.child("report-Test3.xml").copyFrom(getTest("report-Test3.xml"));
        dir.child("report-Test4.xml").copyFrom(getTest("report-Test4.xml"));
        dir.child("report-Test5.xml").copyFrom(getTest("report-Test5.xml"));
    }

    @Test(timeout = 60000)
    public void testStandardWorkflow() throws Exception {
        String script = ""
                + "node {\n"
                + "   echo 'Starting'\n"
                + "   sh 'pwd && ls -la'\n"
                + "   def splits = splitTests([$class: 'CountDrivenParallelism', size: 5])\n "
                + "   assert splits.size() == 1\n"
                + "   step([$class: 'JUnitResultArchiver', testResults: 'report-*.xml'])\n "
                + "   echo 'Ending'\n"
                + "}\n";
        job.setDefinition(new CpsFlowDefinition(script));
        waitAndAssertSuccess(job.scheduleBuild2(0).waitForStart());
        job.setDefinition(new CpsFlowDefinition(script.replace("== 1", "== 5")));
        waitAndAssertSuccess(job.scheduleBuild2(0).waitForStart());
    }

    @Test(timeout = 60000)
    public void testWorkflowWithArchiveId() throws Exception {
        String script = ""
                + "node {\n"
                + "   echo 'Starting'\n"
                + "   sh 'pwd && ls -la'\n"
                + "   def splits = splitTests(parallelism: [$class: 'CountDrivenParallelism', size: 2], archiveId: 'eleven')\n "
                + "   assert splits.size() == 1\n"
                + "   for(def sp in splits) { println \"one ${splits.size()}:${sp.size()}\"\nprintln sp.join('\\n')}\n"
                + "   //splits = splitTests(parallelism: [$class: 'CountDrivenParallelism', size: 5], archiveId: 'notThere')\n "
                + "   //assert splits.size() == 1\n"
                + "   step([$class: 'JUnitResultArchiver', testResults: 'report-*Test1?.xml', archiveId: 'eleven'])\n "
                + "   step([$class: 'JUnitResultArchiver', testResults: 'report-*Test3.xml', archiveId: 'three'])\n "
                + "   step([$class: 'JUnitResultArchiver', testResults: 'report-*Test4.xml'])\n "
                + "   step([$class: 'JUnitResultArchiver', testResults: 'report-*Test5.xml', archiveId: 'five'])\n "
                + "   echo 'Ending'\n"
                + "}\n";
        job.setDefinition(new CpsFlowDefinition(script));
        waitAndAssertSuccess(job.scheduleBuild2(0).waitForStart());
        job.setDefinition(new CpsFlowDefinition(script.replaceFirst("== 1", "== 2")
                .replace("//splits", "splits")
                .replace("//assert", "assert")));
        WorkflowRun run = waitAndAssertSuccess(job.scheduleBuild2(0).waitForStart());
        j.assertLogContains("Test1.java", run);
        j.assertLogContains("Test2.java", run);
        j.assertLogNotContains("Test3.java", run);
    }

    private WorkflowRun waitAndAssertSuccess(WorkflowRun run) throws Exception {
        while (run.getResult() == null) {
            System.out.println("Waiting for "+run.getDisplayName()+" to complete...");
            Thread.sleep(1000);
        }
        try {
            assertSame(Result.SUCCESS, run.getResult());
        } catch (AssertionError e) {
            run.getLogText().writeLogTo(0, System.out);
            throw e;
        }
        return run;
    }

    private InputStream getTest(String localName) {
        return getClass().getResourceAsStream("/org/jenkinsci/plugins/parallel_test_executor/ParallelTestExecutorUnitTest/findTestSplits/" + localName);
    }
}