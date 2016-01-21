package org.jenkinsci.plugins.parallel_test_executor;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Allows the splitting logic to be accessed from a workflow.
 */
public final class SplitStep extends AbstractStepImpl {

    private final Parallelism parallelism;

    private boolean generateInclusions;
    private TestMode testMode;

    @DataBoundConstructor public SplitStep(Parallelism parallelism) {
        this.parallelism = parallelism;
    }

    public Parallelism getParallelism() {
        return parallelism;
    }

    public boolean isGenerateInclusions() { return generateInclusions; }

    @DataBoundSetter
    public void setGenerateInclusions(boolean generateInclusions) {
        this.generateInclusions = generateInclusions;
    }
    
    public TestMode getTestMode() {
        if (testMode == null) {
            return TestMode.JAVA;
        } else {
            return testMode;
        }
    }
    
    @DataBoundSetter
    public void setTestMode(TestMode testMode) {
        this.testMode = testMode;
    }

    @Extension public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override public String getFunctionName() {
            return "splitTests";
        }

        @Override public String getDisplayName() {
            return "Split Test Runs";
        }

    }

    public static final class Execution extends AbstractSynchronousStepExecution<List<?>> {

        private static final long serialVersionUID = 1L;

        @Inject private transient SplitStep step;
        @StepContextParameter private transient Run<?,?> build;
        @StepContextParameter private transient TaskListener listener;

        @Override protected List<?> run() throws Exception {
            if (step.generateInclusions) {
                return ParallelTestExecutor.findTestSplits(step.parallelism, build, listener, step.generateInclusions, step.getTestMode());
            } else {
                List<List<String>> result = new ArrayList<>();
                for (InclusionExclusionPattern pattern : ParallelTestExecutor.findTestSplits(step.parallelism, build, listener, step.generateInclusions, step.getTestMode())) {
                    result.add(pattern.getList());
                }
                return result;
            }
        }

    }

}
