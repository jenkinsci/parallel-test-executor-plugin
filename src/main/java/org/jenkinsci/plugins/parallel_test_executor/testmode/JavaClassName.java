package org.jenkinsci.plugins.parallel_test_executor.testmode;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.tasks.junit.ClassResult;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.parallel_test_executor.TestClass;
import org.jenkinsci.plugins.parallel_test_executor.TestEntity;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This mode works best with java projects.<br/>
 * Each exclusion/inclusion generates two lines by replacing "." with "/" in the fully qualified test
 * class name and appending ".java" to one line and ".class" to the second line.
 *
 * It is also able to estimate tests to run from the workspace content if no historical context could be found.
 */
public class JavaClassName extends TestMode {
    private static final String PATTERNS = String.join(",", List.of(
            "**/src/test/java/**/Test*.java",
            "**/src/test/java/**/*Test.java",
            "**/src/test/java/**/*Tests.java",
            "**/src/test/java/**/*TestCase.java"
    ));
    private static final Pattern TEST = Pattern.compile(".+/src/test/java/(.+)[.]java");

    @DataBoundConstructor
    public JavaClassName() {}

    @Override
    @NonNull
    public Map<String, TestEntity> getTestEntitiesMap(@NonNull ClassResult classResult) {
        TestClass testClass = new TestClass(classResult);
        return Map.of(testClass.getKey(), testClass);
    }

    @Override
    public Map<String, TestEntity> estimate(FilePath workspace, @NonNull TaskListener listener) throws InterruptedException {
        if (workspace == null) {
            return Map.of();
        }
        Map<String, TestEntity> data = new TreeMap<>();
        try {
            for (FilePath test : workspace.list(PATTERNS)) {
                String testPath = test.getRemote().replace('\\', '/');
                Matcher m = TEST.matcher(testPath);
                if (!m.matches() || m.groupCount() != 1) {
                    throw new IllegalStateException(testPath + " didn't match expected format");
                }
                String relativePath = m.group(1); // e.g. pkg/subpkg/SomeTest
                data.put(relativePath, new TestClass(relativePath));
            }
        } catch (IOException e) {
            e.printStackTrace(listener.error("Unable to determine tests to run from files"));
        }
        return data;
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
