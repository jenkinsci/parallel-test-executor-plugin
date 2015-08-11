package org.jenkinsci.plugins.parallel_test_executor;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.List;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Allows the splitting logic to be accessed from a workflow.
 */
public final class SplitStep extends AbstractStepImpl {

    private final Parallelism parallelism;

    @DataBoundConstructor public SplitStep(Parallelism parallelism) {
        this.parallelism = parallelism;
    }

    public Parallelism getParallelism() {
        return parallelism;
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

    public static final class Execution extends AbstractSynchronousStepExecution<List<InclusionExclusionPattern>> {

        @Inject private SplitStep step;
        @StepContextParameter private Run<?,?> build;
        @StepContextParameter private TaskListener listener;

        @Override protected List<InclusionExclusionPattern> run() throws Exception {
            return ParallelTestExecutor.findTestSplits(step.parallelism, build, listener, false);
        }

    }

}
