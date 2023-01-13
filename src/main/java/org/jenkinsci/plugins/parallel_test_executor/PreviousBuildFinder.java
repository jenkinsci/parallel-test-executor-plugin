package org.jenkinsci.plugins.parallel_test_executor;

import hudson.ExtensionPoint;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * Finds a previous build to look test results for.
 */
public interface PreviousBuildFinder extends ExtensionPoint {
    Run<?,?> find(Run<?, ?> b, TaskListener listener);
}
