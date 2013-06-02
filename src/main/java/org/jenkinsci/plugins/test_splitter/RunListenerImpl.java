package org.jenkinsci.plugins.test_splitter;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.Run.RunnerAbortedException;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Looks fo {@Link TestCollectionMarker} in the build and collect the test reports.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class RunListenerImpl extends RunListener<AbstractBuild<?,?>> {
//    @Override
//    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, RunnerAbortedException {
//        TestCollectionMarker m = build.getAction(TestCollectionMarker.class);
//        if (m!=null)
//            m.clean(build,listener);
//        return super.setUpEnvironment(build, launcher, listener);
//    }

    @Override
    public void onCompleted(AbstractBuild<?,?> build, @Nonnull TaskListener listener) {
        TestCollectionMarker m = build.getAction(TestCollectionMarker.class);
        if (m!=null)
            m.collect(build,listener);
    }
}
