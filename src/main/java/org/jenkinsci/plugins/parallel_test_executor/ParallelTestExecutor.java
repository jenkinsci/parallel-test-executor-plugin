package org.jenkinsci.plugins.parallel_test_executor;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
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
import hudson.plugins.parameterizedtrigger.BinaryFileParameterFactory;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.BuildInfoExporterAction;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TabulatedResult;
import hudson.tasks.test.TestResult;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Kohsuke Kawaguchi
 */
public class ParallelTestExecutor extends Builder {
    private Parallelism parallelism;

    private String testJob;
    private String patternFile;
    private String testReportFiles;

    @DataBoundConstructor
    public ParallelTestExecutor(Parallelism parallelism, String testJob, String patternFile, String testReportFiles) {
        this.parallelism = parallelism;
        this.testJob = testJob;
        this.patternFile = patternFile;
        this.testReportFiles = testReportFiles;
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

    public String getTestReportFiles() {
        return testReportFiles;
    }

    /**
     * {@link TestClass}es are divided into multiple sets of roughly equal size.
     */
    class Knapsack implements Comparable<Knapsack> {
        /**
         * Total duration of all {@link TestClass}es that are in this knapsack.
         */
        long total;

        void add(TestClass tc) {
            assert tc.knapsack==null;
            tc.knapsack=this;
            total+=tc.duration;
        }

        public int compareTo(Knapsack that) {
            long l = this.total - that.total;
            if (l<0)    return -1;
            if (l>0)    return 1;
            return 0;
        }
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        FilePath dir = build.getWorkspace().child("test-splits");
        dir.deleteRecursive();

        BuildInfoExporterAction a = findPreviousTriggerBuild(build);
        if (a == null) {
            listener.getLogger().println("No record available, so executing everything in one place");
            dir.child("split.1.txt").write("", "UTF-8"); // no exclusions
        } else {

            Map<String/*fully qualified class name*/, TestClass> data = new HashMap<String, TestClass>();

            for (AbstractBuild<?, ?> b : a.getTriggeredBuilds()) {
                AbstractTestResultAction tra = b.getTestResultAction();
                if (tra == null)
                    tra = b.getAggregatedTestResultAction();

                if (tra == null)
                    continue;   // nothing to look into

                Object r = tra.getResult();
                if (r instanceof TestResult) {
                    collect((TestResult) r, data);
                }
            }

            // sort in the descending order of the duration
            List<TestClass> sorted = new ArrayList<TestClass>(data.values());
            Collections.sort(sorted);

            // degree of the parallelism. we need minimum 1
            final int n = Math.max(1, parallelism.calculate(sorted));

            List<Knapsack> knapsacks = new ArrayList<Knapsack>(n);
            for (int i = 0; i < n; i++)
                knapsacks.add(new Knapsack());

            /*
                This packing problem is a NP-complete problem, so we solve
                this simply by a greedy algorithm. We pack heavier items first,
                and the result should be of roughly equal size
             */
            PriorityQueue<Knapsack> q = new PriorityQueue<Knapsack>(knapsacks);
            for (TestClass d : sorted) {
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
            listener.getLogger().printf("%d test classes (%dms) divided into %d sets. Min=%dms, Average=%dms, Max=%dms, stddev=%dms\n",
                    data.size(), total, n, min, average, max, stddev);

            // write out exclusion list
            for (int i = 0; i < n; i++) {
                PrintWriter w = new PrintWriter(new BufferedOutputStream(dir.child("split." + i + ".txt").write()));
                Knapsack k = knapsacks.get(i);
                for (TestClass d : sorted) {
                    if (d.knapsack == k) continue;
                    w.println(d.getSourceFileName());
                }
                w.close();
            }
        }

        createTriggerBuilder().perform(build,launcher,listener);

        tally(build, launcher, listener);

        return true;
    }

    /**
     * Collects all the test reports
     */
    private void tally(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        // TODO: offer up the configuration of this, or perhaps let the user configure it separately
        new JUnitResultArchiver("test-splits/reports/**/*.xml").perform(build,launcher,listener);
    }

    /**
     * Create {@link TriggerBuilder} for launching test jobs.
     */
    private TriggerBuilder createTriggerBuilder() {
        // to let the caller job do a clean up, don't let the failure in the test job early-terminate the build process
        // that's why the first argument is ABORTED.
        BlockingBehaviour blocking = new BlockingBehaviour(Result.ABORTED, Result.UNSTABLE, Result.FAILURE);
        final AtomicInteger iota = new AtomicInteger(0);

        // actual logic of child process triggering is left up to the parameterized build
        BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig(testJob,
            blocking,
            Collections.<AbstractBuildParameterFactory>singletonList(
                new BinaryFileParameterFactory(getPatternFile(),"test-splits/split.*.txt")),
            Collections.<AbstractBuildParameters>singletonList(
                // put a marker action that we look for to collect test reports
                new AbstractBuildParameters() {
                    @Override
                    public Action getAction(AbstractBuild<?,?> build, TaskListener listener) throws IOException, InterruptedException, DontTriggerException {
                        return new TestCollector(build, ParallelTestExecutor.this, iota.incrementAndGet());
                    }
                }
            )
        );

        return new TriggerBuilder(config);
    }


    private long pow(long l) {
        return l*l;
    }

    /**
     * Recursive visits the structure inside {@link TestResult}.
     */
    private void collect(TestResult r, Map<String, TestClass> data) {
        if (r instanceof ClassResult) {
            ClassResult cr = (ClassResult) r;
            TestClass dp = new TestClass(cr);
            data.put(dp.className, dp);
            return; // no need to go deeper
        }
        if (r instanceof TabulatedResult) {
            TabulatedResult tr = (TabulatedResult) r;
            for (TestResult child : tr.getChildren()) {
                collect(child, data);
            }
        }
    }

    private BuildInfoExporterAction findPreviousTriggerBuild(AbstractBuild<?,?> b) {
        for (int i=0; i<10; i++) {// limit the search to a small number to avoid loading too much
            b = b.getPreviousBuild();
            if (b==null)    break;

            BuildInfoExporterAction a = b.getAction(BuildInfoExporterAction.class);
            if (a!=null)    return a;
        }
        return null;    // couldn't find it
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public AutoCompletionCandidates doAutoCompleteTestJob(@QueryParameter String value,  @AncestorInPath Item self, @AncestorInPath ItemGroup container) {
            return AutoCompletionCandidates.ofJobNames(AbstractProject.class, value, self, container);
        }

        @Override
        public String getDisplayName() {
            return "Parallel test job execution";
        }
    }
}
