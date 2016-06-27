package org.jenkinsci.plugins.parallel_test_executor;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.tasks.Shell;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.junit.TestResultAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TouchBuilder;
import org.jvnet.hudson.test.recipes.LocalData;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParallelTestExecutorTest {

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
    public void alternateJobInFreestyle() throws Exception {
        FreeStyleProject alternateJob = jenkinsRule.createFreeStyleProject("alternateJob");
        alternateJob.setScm(new ExtractResourceSCM(getClass().getResource("preloadedResults.zip")));
        alternateJob.getBuildersList().add(new TouchBuilder());
        alternateJob.getPublishersList().add(new JUnitResultArchiver("*.xml"));
        FreeStyleBuild alternateRun = jenkinsRule.assertBuildStatusSuccess(alternateJob.scheduleBuild2(0));
        jenkinsRule.assertBuildStatusSuccess(alternateJob.scheduleBuild2(0));


        FreeStyleProject splitJob = jenkinsRule.createFreeStyleProject("splitJob");
        splitJob.setConcurrentBuild(true);
        splitJob.setScm(new ExtractResourceSCM(getClass().getResource("preloadedResults.zip")));
        splitJob.getBuildersList().add(new TouchBuilder());
        splitJob.getBuildersList().add(new Shell("#!/bin/bash\n"
                + "mkdir -p results;"
                + "cp report-Test${BUILD_NUMBER}.xml results")
        );

        FreeStyleProject mainJob = jenkinsRule.createFreeStyleProject("mainJob");
        ParallelTestExecutor toRun = new ParallelTestExecutor(new CountDrivenParallelism(5), "splitJob", "excludes.txt",
                "results/*.xml", true, Collections.<AbstractBuildParameters>emptyList());
        toRun.setAlternateJob("alternateJob");
        mainJob.getBuildersList().add(new Shell("echo 'pants'"));
        mainJob.getBuildersList().add(toRun);

        FreeStyleBuild mainBuild = jenkinsRule.assertBuildStatusSuccess(mainJob.scheduleBuild2(0));
        jenkinsRule.assertLogContains("divided into 5 sets.", mainBuild);

        assertEquals(alternateRun.getAction(TestResultAction.class).getTotalCount(),
                mainBuild.getAction(TestResultAction.class).getTotalCount());

        assertEquals(6, splitJob.getNextBuildNumber());
    }

    @Test
    public void workflowGenerateInclusions() throws Exception {
        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "def splits = splitTests parallelism: [$class: 'CountDrivenParallelism', size: 2], generateInclusions: true\n" +
            "echo \"splits.size=${splits.size()}\"; for (int i = 0; i < splits.size(); i++) {\n" +
            "  def split = splits[i]; echo \"splits[${i}]: includes=${split.includes} list=${split.list}\"\n" +
            "}\n" +
            "node {\n" +
            "  writeFile file: 'TEST-1.xml', text: '<testsuite name=\"one\"><testcase name=\"x\"/></testsuite>'\n" +
            "  writeFile file: 'TEST-2.xml', text: '<testsuite name=\"two\"><testcase name=\"y\"/></testsuite>'\n" +
            "  step([$class: 'JUnitResultArchiver', testResults: 'TEST-*.xml'])\n" +
            "}", true));
        WorkflowRun b1 = jenkinsRule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        jenkinsRule.assertLogContains("splits.size=1", b1);
        jenkinsRule.assertLogContains("splits[0]: includes=false list=[]", b1);
        WorkflowRun b2 = jenkinsRule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        jenkinsRule.assertLogContains("splits.size=2", b2);
        jenkinsRule.assertLogContains("splits[0]: includes=false list=[two.java, two.class]", b2);
        jenkinsRule.assertLogContains("splits[1]: includes=true list=[two.java, two.class]", b2);
    }

}
