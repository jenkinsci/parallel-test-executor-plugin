package org.jenkinsci.plugins.parallel_test_executor;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.plugins.parameterizedtrigger.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TabulatedResult;
import hudson.tasks.test.TestResult;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.Charsets;

import javax.annotation.CheckForNull;

/**
 * @author Kohsuke Kawaguchi
 */
public class ParallelTestExecutor extends Builder {
    public static final int NUMBER_OF_BUILDS_TO_SEARCH = 20;
    public static final ImmutableSet<Result> RESULTS_OF_BUILDS_TO_CONSIDER = ImmutableSet.of(Result.SUCCESS, Result.UNSTABLE);
    private Parallelism parallelism;

    private String testJob;
    private String patternFile;
    private String includesPatternFile;
    private String testReportFiles;
    private boolean doNotArchiveTestResults = false;
    private List<AbstractBuildParameters> parameters;
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

    public List<AbstractBuildParameters> getParameters() {
        return parameters;
    }

    /**
     * {@link org.jenkinsci.plugins.parallel_test_executor.TestEntity}es are divided into multiple sets of roughly equal size.
     */
    @SuppressFBWarnings(value="EQ_COMPARETO_USE_OBJECT_EQUALS", justification="We wish to consider knapsacks as distinct items, just sort by size.")
    static class Knapsack implements Comparable<Knapsack> {
        /**
         * Total duration of all {@link org.jenkinsci.plugins.parallel_test_executor.TestEntity}es that are in this knapsack.
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
        List<InclusionExclusionPattern> splits = findTestSplits(parallelism, build, listener, includesPatternFile != null, getTestMode());
        for (int i = 0; i < splits.size(); i++) {
            InclusionExclusionPattern pattern = splits.get(i);
            try (OutputStream os = dir.child("split." + i + "." + (pattern.isIncludes() ? "include" : "exclude") + ".txt").write();
                 OutputStreamWriter osw = new OutputStreamWriter(os, Charsets.UTF_8);
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

    static List<InclusionExclusionPattern> findTestSplits(Parallelism parallelism, Run<?,?> build, TaskListener listener, boolean generateInclusions, TestMode testMode) {
        TestResult tr = findPreviousTestResult(build, listener);
        if (tr == null) {
            listener.getLogger().println("No record available, so executing everything in one place");
            return Collections.singletonList(new InclusionExclusionPattern(Collections.<String>emptyList(), false));
        } else {
            Map<String/*fully qualified class/case name*/, TestEntity> data = new TreeMap<>();
            collect(tr, data, testMode);

            // sort in the descending order of the duration
            List<TestEntity> sorted = new ArrayList<>(data.values());
            Collections.sort(sorted);
            
            // degree of the parallelism. we need minimum 1
            final int n = Math.max(1, parallelism.calculate(sorted));

            List<Knapsack> knapsacks = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
                knapsacks.add(new Knapsack());

            /*
                This packing problem is a NP-complete problem, so we solve
                this simply by a greedy algorithm. We pack heavier items first,
                and the result should be of roughly equal size
             */
            PriorityQueue<Knapsack> q = new PriorityQueue<>(knapsacks);
            for (TestEntity d : sorted) {
                Knapsack k = q.poll();
                k.add(d);
                q.add(k);
            }

            long total = 0, min = Long.MAX_VALUE, max = Long.MIN_VALUE;
            for (Knapsack k : knapsacks) {
                total += k.total;
                max = Math.max(max, k.total);
                min = Math.min(min, k.total);
            }
            long average = total / n;
            long variance = 0;
            for (Knapsack k : knapsacks) {
                variance += pow(k.total - average);
            }
            variance /= n;
            long stddev = (long) Math.sqrt(variance);
            listener.getLogger().printf("%d test %s (%dms) divided into %d sets. Min=%dms, Average=%dms, Max=%dms, stddev=%dms%n",
                    data.size(), testMode == TestMode.JAVA ? "classes" : "cases", total, n, min, average, max, stddev);

            List<InclusionExclusionPattern> r = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Knapsack k = knapsacks.get(i);
                boolean shouldIncludeElements = generateInclusions && i != 0;
                List<String> elements = new ArrayList<>();
                r.add(new InclusionExclusionPattern(elements, shouldIncludeElements));
                for (TestEntity d : sorted) {
                    if (shouldIncludeElements == (d.knapsack == k)) {
                        elements.addAll(d.getOutputString());
                    }
                }
            }
            return r;
        }
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


    private static long pow(long l) {
        return l * l;
    }

    /**
     * Recursive visits the structure inside {@link hudson.tasks.test.TestResult}.
     */
    static private void collect(TestResult r, Map<String, TestEntity> data, TestMode testMode) {
        if (r instanceof ClassResult) {
            ClassResult classResult = (ClassResult) r;
            if(testMode == TestMode.JAVA) {
                TestClass dp = new TestClass(classResult);
                data.put(dp.className, dp);
            } else {
                for(CaseResult caseResult : classResult.getChildren()) {
                    TestCase dp = new TestCase(caseResult, testMode == TestMode.CLASSANDTESTCASENAME);
                    data.put(dp.output, dp);
                }
            }
            return; // no need to go deeper
        }
        if (r instanceof TabulatedResult) {
            TabulatedResult tr = (TabulatedResult) r;
            for (TestResult child : tr.getChildren()) {
                collect(child, data, testMode);
            }
        }
    }

    private static TestResult findPreviousTestResult(Run<?, ?> b, TaskListener listener) {
        for (int i = 0; i < NUMBER_OF_BUILDS_TO_SEARCH; i++) {// limit the search to a small number to avoid loading too much
            b = b.getPreviousBuild();
            if (b == null) break;
            if(!RESULTS_OF_BUILDS_TO_CONSIDER.contains(b.getResult())) continue;

            AbstractTestResultAction tra = b.getAction(AbstractTestResultAction.class);
            if (tra == null) continue;

            Object o = tra.getResult();
            if (o instanceof TestResult) {
                listener.getLogger().printf("Using build #%d as reference%n", b.getNumber());
                return (TestResult) o;
            }
        }
        return null;    // couldn't find it
    }

    @Extension
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
