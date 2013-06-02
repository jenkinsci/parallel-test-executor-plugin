package org.jenkinsci.plugins.parallel_test_executor;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class CountDrivenParallelism extends Parallelism {
    public int size;

    @DataBoundConstructor
    public CountDrivenParallelism(int size) {
        this.size = size;
    }

    @Override
    public int calculate(List<TestClass> tests) {
        return size;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Parallelism> {
        @Override
        public String getDisplayName() {
            return "Fixed number of batches";
        }
    }
}
