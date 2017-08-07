package org.jenkinsci.plugins.parallel_test_executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import hudson.model.FreeStyleProject;
import jenkins.branch.BranchBuildStrategy;
import jenkins.branch.BranchSource;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.SnippetizerTester;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import java.util.ArrayList;

public class ParallelTestExecutorTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    @Rule
    public GitSampleRepoRule sampleRepo = new GitSampleRepoRule();

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
    public void multiBranchFallbackToPrimaryJobForFirstBuild() throws Exception {
        // verifies that a first time build of a project falls back to the primary branch

        // - initialize a git repo
        // - checkout the branch "primary-branch"
        // - add new MultiBranch project "p" to Jenkins
        // - trigger branch indexing, but don't build branches automatically
        // - build primary-branch#1 and let it generate test results
        // - build test-branch#1, see it using test results from primary-branch#1
        // - build test-branch#2, see it using test results from test-branch#1

        WorkflowMultiBranchProject multiBranchProject = getMultiBranchProjectForFallbackTests();
        WorkflowJob primaryBranch = multiBranchProject.getItem("primary-branch");
        WorkflowJob testBranch = multiBranchProject.getItem("test-branch");

        // - build primary-branch#1 and let it generate test results
        primaryBranch.setDefinition(getPipelineScriptWithTestResults());
        build(primaryBranch);
        assertEquals(1, primaryBranch.getLastBuild().getNumber());

        // - build test-branch#1, see it using test results from primary-branch#1
        build(testBranch);
        WorkflowRun testbranchBuild1 = testBranch.getLastBuild();
        assertEquals(1, testbranchBuild1.getNumber());
        jenkinsRule.assertLogContains("Scanning primary project for test records. Starting with build p/primary-branch #1", testbranchBuild1);
        // check that we actually pick p/primary-branch#2 because it has test results
        jenkinsRule.assertLogContains("Using build #1 as reference", testbranchBuild1);

        // - build test-branch#2, see it using test results from test-branch#1
        build(testBranch);
        WorkflowRun testbranchBuild2 = testBranch.getLastBuild();
        assertEquals(2, testbranchBuild2.getNumber());
        jenkinsRule.assertLogContains("Scanning primary project for test records. Starting with build p/primary-branch #1", testbranchBuild2);
        // check that we actually pick p/primary-branch#2 because it has test results
        jenkinsRule.assertLogContains("Using build #1 as reference", testbranchBuild2);
    }

    @Test
    public void multiBranchFallbackToPrimaryJobForSecondBuild() throws Exception {

        // verifies that builds of a project with a history lacking test results falls back to the primary branch

        // - initialize a git repo
        // - checkout the branch "primary-branch"
        // - add new MultiBranch project "p" to Jenkins
        // - trigger branch indexing, but don't build branches automatically
        // - build test-branch#1, see it missing test results, let it generate no test results
        // - build primary-branch#1 and let it generate test results
        // - build primary-branch#2 and let it generate NO test results
        // - build test-branch#2, see it using test results from primary-branch#1, let it generate no test results

        WorkflowMultiBranchProject multiBranchProject = getMultiBranchProjectForFallbackTests();
        WorkflowJob primaryBranch = multiBranchProject.getItem("primary-branch");
        WorkflowJob testBranch = multiBranchProject.getItem("test-branch");

        // - build test-branch#1, see it missing test results, let it generate no test results
        build(testBranch);
        WorkflowRun testbranchBuild1 = testBranch.getLastBuild();
        assertEquals(1, testbranchBuild1.getNumber());
        jenkinsRule.assertLogContains("No record available, so executing everything in one place", testbranchBuild1);

        // - build primary-branch#1 and let it generate test results
        primaryBranch.setDefinition(getPipelineScriptWithTestResults());
        build(primaryBranch);
        assertEquals(1, primaryBranch.getLastBuild().getNumber());

        // - build primary-branch#2 and let it generate NO test results
        primaryBranch.setDefinition(new CpsFlowDefinition("echo 'no test results'", true));
        build(primaryBranch);
        assertEquals(2, primaryBranch.getLastBuild().getNumber());

        // - build test-branch#2, see it using test results from primary-branch#1, let it generate no test results
        build(testBranch);
        WorkflowRun testbranchBuild2 = testBranch.getLastBuild();
        assertEquals(2, testbranchBuild2.getNumber());
        jenkinsRule.assertLogContains("Scanning primary project for test records. Starting with build p/primary-branch #2", testbranchBuild2);
        // check that we actually pick p/primary-branch#2 because it has test results
        jenkinsRule.assertLogContains("Using build #1 as reference", testbranchBuild2);
    }

    private CpsFlowDefinition getPipelineScriptWithTestResults() {
        return new CpsFlowDefinition(
            "node {\n" +
                "  writeFile file: 'TEST-1.xml', text: '<testsuite name=\"one\"><testcase name=\"x\"/></testsuite>'\n" +
                "  writeFile file: 'TEST-2.xml', text: '<testsuite name=\"two\"><testcase name=\"y\"/></testsuite>'\n" +
                "  junit 'TEST-*.xml'\n" +
                "}", true);
    }

    private void build(WorkflowJob project) throws Exception {
        project.scheduleBuild2(0);
        jenkinsRule.waitUntilNoActivity();
    }

    private WorkflowMultiBranchProject getMultiBranchProjectForFallbackTests() throws Exception {
        // create a Jenkinsfile in the master branch
        sampleRepo.init();
        String script =
                "def splits = splitTests parallelism: count(2), generateInclusions: true\n" +
                "echo \"branch=${env.BRANCH_NAME}\"\n" +
                        "node {\n" +
                        "  checkout scm\n" +
                        "}";
        sampleRepo.write("Jenkinsfile", script);
        sampleRepo.git("add", "Jenkinsfile");
        sampleRepo.git("commit", "--all", "--message=flow");
        // create a new branch based on master
        sampleRepo.git("branch", "test-branch");
        // checkout a new branch that will get PrimaryInstanceMetadataAction because of it is checked out when indexing
        sampleRepo.git("checkout", "-b", "primary-branch");

        // create MultiBranch project "p"
        WorkflowMultiBranchProject multiBranchProject = jenkinsRule.jenkins.createProject(WorkflowMultiBranchProject.class, "p");
        BranchSource branchSource = new BranchSource(new GitSCMSource(null, sampleRepo.toString(), "", "*", "", false));
        ArrayList<BranchBuildStrategy> buildStrategies = new ArrayList<>();
        buildStrategies.add(new DontBuildBranchBuildStrategy());
        branchSource.setBuildStrategies(buildStrategies);
        multiBranchProject.getSourcesList().add(branchSource);
        // indexing will automatically trigger a run for every branch
        multiBranchProject.scheduleBuild2(0).getFuture().get();
        jenkinsRule.waitUntilNoActivity();
        // MultiBranch project should have 3 items (master, test-branch, primary-branch)
        assertEquals(3, multiBranchProject.getItems().size());

        return multiBranchProject;
    }
}
