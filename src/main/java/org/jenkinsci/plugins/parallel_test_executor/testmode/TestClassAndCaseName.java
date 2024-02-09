package org.jenkinsci.plugins.parallel_test_executor.testmode;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Each exclusion/inclusion generates one line consisting of the class
 * and test case name on a <em>className.testName</em> format.
 */
public class TestClassAndCaseName extends TestCaseName {

    @DataBoundConstructor
    public TestClassAndCaseName() {}

    @Override
    public boolean isIncludeClassName() {
        return true;
    }

    @Extension
    @Symbol("qualifiedTestCase")
    public static class DescriptorImpl extends Descriptor<TestMode> {
        @Override
        @NonNull
        public String getDisplayName() {
            return "By test class and case name";
        }
    }
}
