package org.jenkinsci.plugins.parallel_test_executor.testmode;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.tasks.junit.ClassResult;
import java.util.Map;
import org.jenkinsci.plugins.parallel_test_executor.TestEntity;

public abstract class TestMode extends AbstractDescribableImpl<TestMode> implements ExtensionPoint {
    /**
     * @param classResult The initial class result
     * @return a Map of test entities, keyed by their unique key
     */
    @NonNull
    public abstract Map<String, TestEntity> getTestEntitiesMap(@NonNull ClassResult classResult);

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
