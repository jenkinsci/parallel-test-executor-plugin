package org.jenkinsci.plugins.parallel_test_executor.testmode;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import java.util.HashMap;
import java.util.Map;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.parallel_test_executor.TestCase;
import org.jenkinsci.plugins.parallel_test_executor.TestEntity;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Each exclusion/inclusion generates one line consisting of the test case name only. <br/>
 * This is useful where a tool produces JUnit result XML containing unique test case names without any class prefix.
 */
public class TestCaseName extends TestMode {

    @DataBoundConstructor
    public TestCaseName() {
    }

    public boolean isIncludeClassName() {
        return false;
    }

    @NonNull
    @Override
    public Map<String, TestEntity> getTestEntitiesMap(@NonNull ClassResult classResult) {
        var result = new HashMap<String, TestEntity>();
        for (CaseResult caseResult : classResult.getChildren()) {
            var testCase = new TestCase(caseResult, isIncludeClassName());
            result.put(testCase.getKey(), testCase);
        }
        return result;
    }

    @Override
    @NonNull
    public String getWord() {
        return "cases";
    }

    @Extension
    @Symbol("testCase")
    public static class DescriptorImpl extends Descriptor<TestMode> {
        @Override
        @NonNull
        public String getDisplayName() {
            return "By test case name";
        }
    }
}
