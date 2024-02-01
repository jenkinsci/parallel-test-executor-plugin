package org.jenkinsci.plugins.parallel_test_executor;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Looks for {@link TestCollector} in the build and collects the test reports.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class RunListenerImpl extends RunListener<AbstractBuild<?,?>> {
    @Override
    public void onCompleted(AbstractBuild<?,?> build, @NonNull TaskListener listener) {
        TestCollector m = build.getAction(TestCollector.class);
        if (m!=null)
            m.collect(build,listener);
    }
}
