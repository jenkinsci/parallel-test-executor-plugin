package org.jenkinsci.plugins.parallel_test_executor;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.TimeUnit2;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class TimeDrivenParallelism extends Parallelism {
    public int mins;

    @DataBoundConstructor
    public TimeDrivenParallelism(int mins) {
        this.mins = mins;
    }

    @Override
    public int calculate(List<TestClass> tests) {
        long total=0;
        for (TestClass test : tests) {
            total += test.duration;
        }
        long chunk = TimeUnit2.MINUTES.toMillis(mins);
        return (int)((total+chunk-1)/chunk);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Parallelism> {
        @Override
        public String getDisplayName() {
            return "Fixed time (minutes) for each batch";
        }
    }
}
