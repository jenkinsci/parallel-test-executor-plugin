package org.jenkinsci.plugins.parallel_test_executor.testmode;

import static java.util.function.Function.identity;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.parallel_test_executor.TestClass;
import org.jenkinsci.plugins.parallel_test_executor.TestEntity;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This mode works best with java projects.
 * <p>
 * Each exclusion/inclusion generates two lines by replacing "." with "/" in the fully qualified test
 * class name and appending ".java" to one line and ".class" to the second line.
 * </p>
 * <p>
 * It is also able to estimate tests to run from the workspace content if no historical context could be found.
 * </p>
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

    public boolean isSplitByCase() {
        return false;
    }

    @Override
    @NonNull
    public Map<String, TestEntity> getTestEntitiesMap(@NonNull ClassResult classResult) {
        if (isSplitByCase()) {
            return classResult.getChildren().stream().map(JavaTestCase::new).collect(Collectors.toMap(JavaTestCase::getKey, identity(), JavaTestCase::new));
        } else {
            TestClass testClass = new TestClass(classResult);
            return Map.of(testClass.getKey(), testClass);
        }
    }

    @Override
    public Map<String, TestEntity> estimate(FilePath workspace, @NonNull TaskListener listener) throws InterruptedException {
        // TODO estimate test cases
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
        return isSplitByCase() ? "cases" : "classes";
    }

    private static class JavaTestCase extends TestEntity {
        private final String output;
        private JavaTestCase(CaseResult cr) {
            // Parameterized tests use ${fqdnClassName}#${methodName}[{parametersDescription}] format
            // passing parameters to surefire is not supported, so just drop them and will sum durations
            this.output = cr.getClassName() + "#" + cr.getName().split("\\[")[0];
            this.duration = (long)(cr.getDuration()*1000);  // milliseconds is a good enough precision for us
        }

        /**
         * Merge two java test cases with the same name, summing their durations.
         */
        private JavaTestCase(TestEntity te1, TestEntity te2) {
            if (!te1.getKey().equals(te2.getKey())) {
                throw new IllegalArgumentException("Test cases must have the same key");
            }
            this.output = te1.getKey();
            this.duration = te1.getDuration() + te2.getDuration();
        }

        @Override
        public String getKey() {
            return output;
        }

        @Override
        public List<String> getElements() {
            return List.of(output);
        }

        @Override
        public String toString() {
            return output;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JavaTestCase that = (JavaTestCase) o;
            return Objects.equals(output, that.output);
        }

        @Override
        public int hashCode() {
            return Objects.hash(output);
        }
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
