package org.jenkinsci.plugins.parallel_test_executor;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.junit.JUnitResultArchiver;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jenkinsci.plugins.parallel_test_executor.testmode.TestMode;
import org.jenkinsci.plugins.variant.OptionalExtension;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * @author Kohsuke Kawaguchi
 */
public class ParallelTestExecutor extends Builder {
    private static final Logger LOGGER = Logger.getLogger(ParallelTestExecutor.class.getName());
    public static final int NUMBER_OF_BUILDS_TO_SEARCH = 20;
    public static final ImmutableSet<Result> RESULTS_OF_BUILDS_TO_CONSIDER = ImmutableSet.of(Result.SUCCESS, Result.UNSTABLE);

    private final Parallelism parallelism;
    private final String testJob;
    private final String patternFile;
    private String includesPatternFile;
    private final String testReportFiles;
    private final boolean doNotArchiveTestResults;
    private final List<AbstractBuildParameters> parameters;
    private TestMode testMode;

    @DataBoundConstructor
    public ParallelTestExecutor(Parallelism parallelism, String testJob, String patternFile, String testReportFiles, boolean archiveTestResults, List<AbstractBuildParameters> parameters) {
        this.parallelism = parallelism;
        this.testJob = testJob;
        this.patternFile = patternFile;
        this.testReportFiles = testReportFiles;
        this.parameters = parameters;
        this.doNotArchiveTestResults = !archiveTestResults;
    }

    public Parallelism getParallelism() {
        return parallelism;
    }

    public String getTestJob() {
        return testJob;
    }

    public String getPatternFile() {
        return patternFile;
    }

    @CheckForNull
    public String getIncludesPatternFile() {
        return includesPatternFile;
    }

    @DataBoundSetter
    public void setIncludesPatternFile(String includesPatternFile) {
        this.includesPatternFile = Util.fixEmpty(includesPatternFile);
    }

    public String getTestReportFiles() {
        return testReportFiles;
    }

    public boolean isArchiveTestResults() {
        return !doNotArchiveTestResults;
    }

    @SuppressWarnings("unused") // jetty
    public TestMode getTestMode() {
        return TestMode.fixDefault(testMode);
    }

    @DataBoundSetter
    public void setTestMode(TestMode testMode) {
        this.testMode = testMode;
    }

    public List<AbstractBuildParameters> getParameters() {
        return parameters;
    }

    /**
     * {@link TestEntity}es are divided into multiple sets of roughly equal size.
     */
    @SuppressFBWarnings(value="EQ_COMPARETO_USE_OBJECT_EQUALS", justification="We wish to consider knapsacks as distinct items, just sort by size.")
    static class Knapsack implements Comparable<Knapsack> {
        /**
         * Total duration of all {@link TestEntity}es that are in this knapsack.
         */
        long total;

        void add(TestEntity tc) {
            assert tc.knapsack == null;
            tc.knapsack = this;
            total += tc.duration;
        }

        public int compareTo(Knapsack that) {
            long l = this.total - that.total;
            if (l < 0) return -1;
            if (l > 0) return 1;
            return 0;
        }
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        FilePath workspace = build.getWorkspace();
        if (workspace == null) {
            throw new AbortException("no workspace");
        }
        FilePath dir = workspace.child("test-splits");
        dir.deleteRecursive();
        List<InclusionExclusionPattern> splits = Splitter.findTestSplits(parallelism, testMode, build, listener, includesPatternFile != null,
                null, build.getWorkspace());
        for (int i = 0; i < splits.size(); i++) {
            InclusionExclusionPattern pattern = splits.get(i);
            try (OutputStream os = dir.child("split." + i + "." + (pattern.isIncludes() ? "include" : "exclude") + ".txt").write();
                 OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                 PrintWriter pw = new PrintWriter(osw)) {
                for (String filePattern : pattern.getList()) {
                    pw.println(filePattern);
                }
            }
        }

        createTriggerBuilder().perform(build, launcher, listener);

        if (isArchiveTestResults()) {
            tally(build, launcher, listener);
        }

        return true;
    }

    /**
     * Collects all the test reports
     */
    private void tally(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        new JUnitResultArchiver("test-splits/reports/**/*.xml", false, null).perform(build, launcher, listener);
    }

    /**
     * Create {@link hudson.plugins.parameterizedtrigger.TriggerBuilder} for launching test jobs.
     */
    private TriggerBuilder createTriggerBuilder() {
        // to let the caller job do a clean up, don't let the failure in the test job early-terminate the build process
        // that's why the first argument is ABORTED.
        BlockingBehaviour blocking = new BlockingBehaviour(Result.ABORTED, Result.UNSTABLE, Result.FAILURE);
        final AtomicInteger iota = new AtomicInteger(0);

        List<AbstractBuildParameters> parameterList = new ArrayList<>();
        parameterList.add(
                // put a marker action that we look for to collect test reports
                new AbstractBuildParameters() {
                    @Override
                    public Action getAction(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException, DontTriggerException {
                        return new TestCollector(build, ParallelTestExecutor.this, iota.incrementAndGet());
                    }
                });
        if (parameters != null) {
            parameterList.addAll(parameters);
        }

        // actual logic of child process triggering is left up to the parameterized build
        List<MultipleBinaryFileParameterFactory.ParameterBinding> parameterBindings = new ArrayList<>();
        parameterBindings.add(new MultipleBinaryFileParameterFactory.ParameterBinding(getPatternFile(), "test-splits/split.*.exclude.txt"));
        if (includesPatternFile != null) {
            parameterBindings.add(new MultipleBinaryFileParameterFactory.ParameterBinding(getIncludesPatternFile(), "test-splits/split.*.include.txt"));
        }
        MultipleBinaryFileParameterFactory factory = new MultipleBinaryFileParameterFactory(parameterBindings);
        BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig(
                testJob,
                blocking,
                Collections.<AbstractBuildParameterFactory>singletonList(factory),
                parameterList
        );

        return new TriggerBuilder(config);
    }

    @OptionalExtension(requirePlugins = "parameterized-trigger")
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public AutoCompletionCandidates doAutoCompleteTestJob(@QueryParameter String value, @AncestorInPath Item self, @AncestorInPath ItemGroup container) {
            return AutoCompletionCandidates.ofJobNames(AbstractProject.class, value, self, container);
        }

        @Override
        public String getDisplayName() {
            return "Parallel test job execution";
        }
    }

}
