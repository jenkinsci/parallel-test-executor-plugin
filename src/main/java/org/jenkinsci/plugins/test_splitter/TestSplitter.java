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
import hudson.tasks.Publisher;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.test.TabulatedResult;
import hudson.tasks.test.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class TestSplitter extends Builder {

    @DataBoundConstructor
    public TestSplitter() {
    }

    /**
     * Execution time of a specific test case.
     */
    class DataPoint implements Comparable<DataPoint> {
        String className;
        long duration;


        public DataPoint(ClassResult cr) {
            String pkgName = cr.getParent().getName();
            if (pkgName.equals("(root)"))   // UGH
                pkgName = "";
            else
                pkgName += '.';
            this.className = pkgName+cr.getName();
            this.duration = (long)(cr.getDuration()*1000);  // milliseconds is a good enough precision for us
        }

        public int compareTo(DataPoint that) {
            long l = this.duration - that.duration;
            // sort them in the descending order
            if (l>0)    return -1;
            if (l<0)    return 1;
            return 0;
        }
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        FilePath dir = build.getWorkspace().child("test-splits");
        dir.deleteRecursive();

        BuildInfoExporterAction a = findPreviousTriggerBuild(build);
        if (a==null) {
            listener.getLogger().println("No record available, so executing everything in one place");
            dir.child("split.1.txt").write("", "UTF-8"); // no exclusions
        } else {

            Map<String/*fully qualified class name*/,DataPoint> data = new HashMap<String, DataPoint>();

            for (AbstractBuild<?,?> b : a.getTriggeredBuilds()) {
                AbstractTestResultAction tra = b.getTestResultAction();
                if (tra==null)
                    tra = b.getAggregatedTestResultAction();

                if (tra==null)
                    continue;   // nothing to look into

                Object r = tra.getResult();
                if (r instanceof TestResult) {
                    collect((TestResult)r,data);
                }
            }

            List<DataPoint> sorted = new ArrayList<DataPoint>(data.values());
            Collections.sort(sorted);

            for (DataPoint d : sorted) {
                listener.getLogger().println(d.className+" "+d.duration);
            }
        }

        return true;
    }

    /**
     * Recursive visits the structure inside {@link TestResult}.
     */
    private void collect(TestResult r, Map<String, DataPoint> data) {
        if (r instanceof ClassResult) {
            ClassResult cr = (ClassResult) r;
            DataPoint dp = new DataPoint(cr);
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
