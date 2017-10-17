package org.jenkinsci.plugins.parallel_test_executor;

import com.google.common.base.Predicate;
import hudson.ExtensionPoint;
import hudson.model.Run;
import hudson.tasks.test.TestResult;
import org.jenkinsci.plugins.variant.OptionalExtension;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graphanalysis.DepthFirstScanner;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.StageStep;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class PreviousTestResultLookup implements ExtensionPoint {
    public abstract TestResult lookupTestResult(@Nonnull TestResult result);

    @OptionalExtension(requirePlugins = {"pipeline-stage-step","workflow-cps","workflow-job"})
    public static class LookupInStage extends PreviousTestResultLookup {
        @CheckForNull
        private String stageName;

        public LookupInStage() {

        }

        public LookupInStage(String stageName) {
            this.stageName = stageName;
        }

        @Override
        public TestResult lookupTestResult(@Nonnull TestResult result) {
            Run<?,?> r = result.getRun();
            if (r instanceof WorkflowRun && stageName != null) {
                FlowExecution execution = ((WorkflowRun) r).getExecution();
                if (execution != null) {
                    DepthFirstScanner scanner = new DepthFirstScanner();
                    FlowNode stageId = scanner.findFirstMatch(execution, new Predicate<FlowNode>() {
                        @Override
                        public boolean apply(@Nullable FlowNode input) {
                            return input instanceof StepStartNode &&
                                    ((StepStartNode) input).getDescriptor() instanceof StageStep.DescriptorImpl &&
                                    input.getDisplayName().equals(stageName);
                        }
                    });
                    if (stageId != null) {
                        return ((hudson.tasks.junit.TestResult) result).getResultForPipelineBlock(r.getExternalizableId(),
                                stageId.getId());
                    }

                }
            }

            return result;
        }
    }
}
