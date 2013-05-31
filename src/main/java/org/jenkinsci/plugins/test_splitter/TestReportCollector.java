package org.jenkinsci.plugins.test_splitter;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.plugins.parameterizedtrigger.BuildInfoExporterAction;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.test.AbstractTestResultAction;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

import static hudson.tasks.BuildStepMonitor.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class TestReportCollector extends Recorder {
    @DataBoundConstructor
    public TestReportCollector() {
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        BuildInfoExporterAction a = build.getAction(BuildInfoExporterAction.class);
        if (a!=null) {
            AggregatedTestResultActionImpl agg = new AggregatedTestResultActionImpl(build);
            for (AbstractBuild<?,?> b : a.getTriggeredBuilds()) {
                AbstractTestResultAction tr = b.getTestResultAction();
                if (tr==null)
                    tr = b.getAggregatedTestResultAction();

                if (tr!=null)
                    agg.add(tr);
            }
            build.addAction(agg);
        }
        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return NONE;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Collect test reports from triggered sub-projects";
        }
    }
}
