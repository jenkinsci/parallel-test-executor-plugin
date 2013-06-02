package org.jenkinsci.plugins.parallel_test_executor;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;

/**
 * Looks fo {@Link TestCollector} in the build and collect the test reports.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class RunListenerImpl extends RunListener<AbstractBuild<?,?>> {
    @Override
    public void onCompleted(AbstractBuild<?,?> build, @Nonnull TaskListener listener) {
        TestCollector m = build.getAction(TestCollector.class);
        if (m!=null)
            m.collect(build,listener);
    }
}
