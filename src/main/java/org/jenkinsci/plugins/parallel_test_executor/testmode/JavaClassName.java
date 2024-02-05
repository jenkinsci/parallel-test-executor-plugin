package org.jenkinsci.plugins.parallel_test_executor.testmode;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.tasks.junit.ClassResult;
import java.util.Map;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.parallel_test_executor.TestClass;
import org.jenkinsci.plugins.parallel_test_executor.TestEntity;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This mode works best with java projects.<br/>
 * Each exclusion/inclusion generates two lines by replacing "." with "/" in the fully qualified test
 * class name and appending ".java" to one line and ".class" to the second line.
 */
public class JavaClassName extends TestMode {
    @DataBoundConstructor
    public JavaClassName() {}

    @Override
    @NonNull
    public Map<String, TestEntity> getTestEntitiesMap(@NonNull ClassResult classResult) {
        TestClass testClass = new TestClass(classResult);
        return Map.of(testClass.getKey(), testClass);
    }

    @Override
    @NonNull
    public String getWord() {
        return "classes";
    }

    @Extension
    @Symbol("javaClass")
    public static class DescriptorImpl extends Descriptor<TestMode> {
        @Override
        @NonNull
        public String getDisplayName() {
            return "By java class name";
        }
    }
}
