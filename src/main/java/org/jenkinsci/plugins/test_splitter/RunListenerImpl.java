package org.jenkinsci.plugins.test_splitter;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;

/**
 * Looks fo {@Link TestCollectionMarker} in the build and collect the test reports.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class RunListenerImpl extends RunListener<AbstractBuild<?,?>> {
    @Override
    public void onCompleted(AbstractBuild<?,?> build, @Nonnull TaskListener listener) {
        TestCollectionMarker m = build.getAction(TestCollectionMarker.class);
        if (m!=null)
            m.collect(build,listener);
    }
}
