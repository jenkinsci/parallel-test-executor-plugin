package org.jenkinsci.plugins.parallel_test_executor.testmode;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This mode works best with java projects.
 * <p>
 * Parallelize per java test case.
 * </p>
 * <p>
 * It is also able to estimate tests to run from the workspace content if no historical context could be found.
 * </p>
 */
public class JavaTestCaseName extends JavaClassName {
    @DataBoundConstructor
    public JavaTestCaseName() {}

    @Override
    public boolean isSplitByCase() {
        return true;
    }

    @Extension
    @Symbol("javaTestCase")
    public static class DescriptorImpl extends Descriptor<TestMode> {
        @Override
        @NonNull
        public String getDisplayName() {
            return "By java test cases";
        }
    }
}
