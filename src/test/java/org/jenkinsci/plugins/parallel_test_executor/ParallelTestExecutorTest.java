package org.jenkinsci.plugins.parallel_test_executor;

import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.SnippetizerTester;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import static org.junit.Assert.assertTrue;

public class ParallelTestExecutorTest {

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    @LocalData
    public void xmlWithNoAddJUnitPublisherIsLoadedCorrectly() throws Exception {
        FreeStyleProject p = (FreeStyleProject) jenkinsRule.jenkins.getItem("old");
        ParallelTestExecutor trigger = (ParallelTestExecutor) p.getBuilders().get(0);

        assertTrue(trigger.isArchiveTestResults());
    }

    @Test
    public void workflowGenerateInclusions() throws Exception {
        new SnippetizerTester(jenkinsRule).assertRoundTrip(new SplitStep(new CountDrivenParallelism(5)), "splitTests count(5)");
        SplitStep step = new SplitStep(new TimeDrivenParallelism(3));
        step.setGenerateInclusions(true);
        new SnippetizerTester(jenkinsRule).assertRoundTrip(step, "splitTests generateInclusions: true, parallelism: time(3)");
        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "def splits = splitTests parallelism: count(2), generateInclusions: true\n" +
            "echo \"splits.size=${splits.size()}\"; for (int i = 0; i < splits.size(); i++) {\n" +
            "  def split = splits[i]; echo \"splits[${i}]: includes=${split.includes} list=${split.list}\"\n" +
            "}\n" +
            "node {\n" +
            "  writeFile file: 'TEST-1.xml', text: '<testsuite name=\"one\"><testcase name=\"x\"/></testsuite>'\n" +
            "  writeFile file: 'TEST-2.xml', text: '<testsuite name=\"two\"><testcase name=\"y\"/></testsuite>'\n" +
            "  junit 'TEST-*.xml'\n" +
            "}", true));
        WorkflowRun b1 = jenkinsRule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        jenkinsRule.assertLogContains("splits.size=1", b1);
        jenkinsRule.assertLogContains("splits[0]: includes=false list=[]", b1);
        WorkflowRun b2 = jenkinsRule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        jenkinsRule.assertLogContains("splits.size=2", b2);
        jenkinsRule.assertLogContains("splits[0]: includes=false list=[two.java, two.class]", b2);
        jenkinsRule.assertLogContains("splits[1]: includes=true list=[two.java, two.class]", b2);
    }

    @Issue("JENKINS-27395")
    @Test
    public void splitTestsWithinStage() throws Exception {
        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "def splits = splitTests parallelism: count(2), generateInclusions: true, stage: 'first'\n" +
                        "echo \"splits.size=${splits.size()}\"; for (int i = 0; i < splits.size(); i++) {\n" +
                        "  def split = splits[i]; echo \"splits[${i}]: includes=${split.includes} list=${split.list}\"\n" +
                        "}\n" +
                        "def allSplits = splitTests parallelism: count(2), generateInclusions: true\n" +
                        "echo \"allSplits.size=${allSplits.size()}\"; for (int i = 0; i < allSplits.size(); i++) {\n" +
                        "  def split = allSplits[i]; echo \"allSplits[${i}]: includes=${split.includes} list=${split.list}\"\n" +
                        "}\n" +
                        "stage('first') {\n" +
                        "  node {\n" +
                        "    writeFile file: 'TEST-1.xml', text: '<testsuite name=\"one\"><testcase name=\"x\"/></testsuite>'\n" +
                        "    writeFile file: 'TEST-2.xml', text: '<testsuite name=\"two\"><testcase name=\"y\"/></testsuite>'\n" +
                        "    junit 'TEST-*.xml'\n" +
                        "  }\n" +
                        "}\n" +
                        "stage('second') {\n" +
                        "  node {\n" +
                        "    writeFile file: 'TEST-3.xml', text: '<testsuite name=\"three\"><testcase name=\"a\"/></testsuite>'\n" +
                        "    writeFile file: 'TEST-4.xml', text: '<testsuite name=\"four\"><testcase name=\"b\"/></testsuite>'\n" +
                        "    junit 'TEST-*.xml'\n" +
                        "  }\n" +
                        "}\n", true));
        WorkflowRun b1 = jenkinsRule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        jenkinsRule.assertLogContains("splits.size=1", b1);
        jenkinsRule.assertLogContains("splits[0]: includes=false list=[]", b1);
        jenkinsRule.assertLogContains("allSplits.size=1", b1);
        jenkinsRule.assertLogContains("allSplits[0]: includes=false list=[]", b1);
        WorkflowRun b2 = jenkinsRule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        jenkinsRule.assertLogContains("splits.size=2", b2);
        jenkinsRule.assertLogContains("splits[0]: includes=false list=[two.java, two.class]", b2);
        jenkinsRule.assertLogContains("splits[1]: includes=true list=[two.java, two.class]", b2);
        jenkinsRule.assertLogContains("allSplits.size=2", b2);
        jenkinsRule.assertLogContains("allSplits[0]: includes=false list=[one.java, one.class, two.java, two.class]", b2);
        jenkinsRule.assertLogContains("allSplits[1]: includes=true list=[one.java, one.class, two.java, two.class]", b2);
    }
}
