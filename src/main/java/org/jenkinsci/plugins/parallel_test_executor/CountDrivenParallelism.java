package org.jenkinsci.plugins.parallel_test_executor;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;
import org.jenkinsci.Symbol;

/**
 * @author Kohsuke Kawaguchi
 */
public class CountDrivenParallelism extends Parallelism {
    public final int size;

    @DataBoundConstructor
    public CountDrivenParallelism(int size) {
        this.size = size;
    }

    @Override
    public int calculate(List<TestClass> tests) {
        // Don't split into 5 buckets if we only have 2 tests etc
        return tests == null ?
               size :
               Math.min(size, tests.size());
    }

    @Symbol("count")
    @Extension
    public static class DescriptorImpl extends Descriptor<Parallelism> {
        @Override
        public String getDisplayName() {
            return "Fixed number of batches";
        }
    }
}
