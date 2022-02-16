package org.jenkinsci.plugins.parallel_test_executor;

import hudson.model.FreeStyleProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
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

    @Test
    @Issue("JENKINS-53172")
    public void workflowDoesNotGenerateInclusionsFromRunningBuild() throws Exception {
        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        ParameterDefinition sleepDef = new StringParameterDefinition("SLEEP", "100", "");
        ParametersDefinitionProperty sleepParamsDef = new ParametersDefinitionProperty(sleepDef);
        p.addProperty(sleepParamsDef);
        /* We need to wait for first build to generate test result before calling splitTests in the second build, So
        * instead of trying to use a quiet period, which is not deterministic and will introduce potential flakiness,
        * we use the existing locking and milestone facilities in pipeline to make sure that
        *  a) The second run waits until the first one has generated tests results by using a lock
        *  b) Once the second build finish the previous one is killed by using milestones
        */
        p.setDefinition(new CpsFlowDefinition(
                "lock('test-results') {\n" +
                "  def splits = splitTests parallelism: count(2), generateInclusions: true\n" +
                "  echo \"splits.size=${splits.size()}\"; for (int i = 0; i < splits.size(); i++) {\n" +
                "    def split = splits[i]; echo \"splits[${i}]: includes=${split.includes} list=${split.list}\"\n" +
                "  }\n" +
                "  node {\n" +
                "    writeFile file: 'TEST-1.xml', text: '<testsuite name=\"one\"><testcase name=\"x\" failures=\"1\"/></testsuite>'\n" +
                "    writeFile file: 'TEST-2.xml', text: '<testsuite name=\"two\"><testcase name=\"y\"/></testsuite>'\n" +
                "    junit 'TEST-*.xml'\n" +
                "    currentBuild.result = 'UNSTABLE'\n" + // Needed due to https://issues.jenkins-ci.org/browse/JENKINS-48178
                "  }\n" +
                "}\n" +
                "milestone 1\n" +
                "sleep time: Integer.valueOf(params.SLEEP), unit: 'SECONDS'\n" +
                "milestone 2", true));
        WorkflowRun b1 = p.scheduleBuild2(0, new ParametersAction(new StringParameterValue("SLEEP", "100"))).waitForStart();
        jenkinsRule.waitForMessage("Lock acquired on", b1);
        WorkflowRun b2 = p.scheduleBuild2(0, new ParametersAction(new StringParameterValue("SLEEP", "0"))).get();
        jenkinsRule.assertLogContains("splits.size=1", b2);
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
