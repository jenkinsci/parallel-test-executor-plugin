package org.jenkinsci.plugins.parallel_test_executor;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jenkinsci.Symbol;

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
        long chunk = TimeUnit.MINUTES.toMillis(mins);
        return (int)((total+chunk-1)/chunk);
    }

    @Symbol("time")
    @Extension
    public static class DescriptorImpl extends Descriptor<Parallelism> {
        @Override
        public String getDisplayName() {
            return "Fixed time (minutes) for each batch";
        }
    }
}
