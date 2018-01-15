package org.jenkinsci.plugins.parallel_test_executor;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
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

    private String stage;

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

    public String getStage() {
        return stage;
    }

    @DataBoundSetter
    public void setStage(String stage) {
        this.stage = stage;
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
            FilePath path = this.getContext().get(FilePath.class);
            if (step.generateInclusions) {
                return ParallelTestExecutor.findTestSplits(step.parallelism, build, listener, step.generateInclusions,
                        step.stage, path);
            } else {
                List<List<String>> result = new ArrayList<>();
                for (InclusionExclusionPattern pattern : ParallelTestExecutor.findTestSplits(step.parallelism, build, listener,
                        step.generateInclusions, step.stage, path)) {
                    result.add(pattern.getList());
                }
                return result;
            }
        }

    }

}
