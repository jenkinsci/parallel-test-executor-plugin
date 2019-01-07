package org.jenkinsci.plugins.parallel_test_executor;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Allows the splitting logic to be accessed from a workflow.
 */
public final class SplitStep extends Step {

    private final Parallelism parallelism;

    private boolean generateInclusions;

    private String stage;

    private boolean estimateTestsFromFiles;

    @DataBoundConstructor
    public SplitStep(Parallelism parallelism) {
        this.parallelism = parallelism;
    }

    public Parallelism getParallelism() {
        return parallelism;
    }

    public boolean isGenerateInclusions() {
        return generateInclusions;
    }

    @DataBoundSetter
    public void setGenerateInclusions(boolean generateInclusions) {
        this.generateInclusions = generateInclusions;
    }

    public boolean isEstimateTestsFromFiles() {
        return estimateTestsFromFiles;
    }

    @DataBoundSetter
    public void setEstimateTestsFromFiles(boolean estimateTestsFromFiles) {
        this.estimateTestsFromFiles = estimateTestsFromFiles;
    }

    public String getStage() {
        return stage;
    }

    @DataBoundSetter
    public void setStage(String stage) {
        this.stage = stage;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(context, this);
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class, Run.class);
        }

        @Override
        public String getFunctionName() {
            return "splitTests";
        }

        @Override
        public String getDisplayName() {
            return "Split Test Runs";
        }
    }

    private static final class Execution extends SynchronousStepExecution<List<?>> {

        private static final long serialVersionUID = 1L;

        private final transient SplitStep step;

        Execution(StepContext context, SplitStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected List<?> run() throws Exception {
            StepContext context = getContext();
            Run<?, ?> build = context.get(Run.class);
            TaskListener listener = context.get(TaskListener.class);
            FilePath path = context.get(FilePath.class);

            if (step.generateInclusions) {
                return ParallelTestExecutor.findTestSplits(step.parallelism, build, listener, step.generateInclusions,
                        step.stage, path, step.estimateTestsFromFiles);
            } else {
                List<List<String>> result = new ArrayList<>();
                for (InclusionExclusionPattern pattern : ParallelTestExecutor.findTestSplits(step.parallelism, build, listener,
                        step.generateInclusions, step.stage, path, step.estimateTestsFromFiles)) {
                    result.add(pattern.getList());
                }
                return result;
            }
        }

    }

}
