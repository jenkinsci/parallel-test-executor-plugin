package org.jenkinsci.plugins.test_splitter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.plugins.parameterizedtrigger.BuildInfoExporterAction;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.test.TabulatedResult;
import hudson.tasks.test.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * @author Kohsuke Kawaguchi
 */
public class TestSplitter extends Builder {
    private Parallelism parallelism;

    @DataBoundConstructor
    public TestSplitter(Parallelism parallelism) {
        this.parallelism = parallelism;
    }

    public Parallelism getParallelism() {
        return parallelism;
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

            // degree of the parallelismm. we need minimum 1
            final int n = Math.max(1,parallelism.calculate(sorted));

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

        return true;
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

        @Override
        public String getDisplayName() {
            return "Split tests";
        }
    }
}
