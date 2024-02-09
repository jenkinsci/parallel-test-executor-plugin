package org.jenkinsci.plugins.parallel_test_executor.testmode;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.TaskListener;
import hudson.tasks.junit.ClassResult;
import java.util.Map;
import org.jenkinsci.plugins.parallel_test_executor.TestEntity;

/**
 * Extension point returning a list of test entities either from previous runs or estimated from the workspace.
 */
public abstract class TestMode extends AbstractDescribableImpl<TestMode> implements ExtensionPoint {
    /**
     * @param classResult The initial class result
     * @return a Map of test entities, keyed by their unique key
     */
    @NonNull
    public abstract Map<String, TestEntity> getTestEntitiesMap(@NonNull ClassResult classResult);

    /**
     * This method will be called if no historical test results can be found. In that case, an estimate can be provided from the workspace content.
     * @param workspace The current directory where tests are expected to be found.
     * @param listener The build listener if any output needs to be logged.
     * @return a Map of test entities, keyed by their unique key
     * @throws InterruptedException if the build get interrupted while executing this method.
     */
    public Map<String, TestEntity> estimate(FilePath workspace, @NonNull TaskListener listener) throws InterruptedException {
        return Map.of();
    }

    /**
     * @return a description of the test entity type that is used for splitting, e.g. "cases"
     */
    @NonNull
    public abstract String getWord();

    /**
     * @return the default implementation for this extension point, if none is defined,
     */
    public static TestMode getDefault() {
        return new JavaClassName();
    }

    public static TestMode fixDefault(TestMode testMode) {
        return testMode instanceof JavaClassName ? null : testMode;
    }
}
