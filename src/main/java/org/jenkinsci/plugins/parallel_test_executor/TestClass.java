package org.jenkinsci.plugins.parallel_test_executor;

import hudson.tasks.junit.ClassResult;
import org.jenkinsci.plugins.parallel_test_executor.ParallelTestExecutor.Knapsack;

/**
 * Execution time of a specific test case.
 */
public class TestClass implements Comparable<TestClass> {
    String className;
    long duration;
    /**
     * Knapsack that this test class belongs to.
     */
    Knapsack knapsack;

    public TestClass(ClassResult cr) {
        String pkgName = cr.getParent().getName();
        if (pkgName.equals("(root)"))   // UGH
            pkgName = "";
        else
            pkgName += '.';
        this.className = pkgName+cr.getName();
        this.duration = (long)(cr.getDuration()*1000);  // milliseconds is a good enough precision for us
    }

    public int compareTo(TestClass that) {
        long l = this.duration - that.duration;
        // sort them in the descending order
        if (l>0)    return -1;
        if (l<0)    return 1;
        return 0;
    }

    public String getSourceFileName(String extension) {
        return className.replace('.','/')+extension;
    }
}
