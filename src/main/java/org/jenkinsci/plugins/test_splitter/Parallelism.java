package org.jenkinsci.plugins.test_splitter;

import hudson.model.AbstractDescribableImpl;

import java.util.List;

/**
 * Strategy that determines how many knapsacks we'll create.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Parallelism extends AbstractDescribableImpl<Parallelism> {
    /*package*/ Parallelism() {}

    public abstract int calculate(List<TestClass> tests);
}
