package org.jenkinsci.plugins.parallel_test_executor;

import hudson.tasks.junit.ClassResult;

/**
 * Execution time of a specific test class.
 */
public class TestClass extends TestEntity {
    String className;

    public TestClass(ClassResult cr) {
        String pkgName = cr.getParent().getName();
        if (pkgName.equals("(root)"))   // UGH
            pkgName = "";
        else
            pkgName += '.';
        this.className = pkgName+cr.getName();
        this.duration = (long)(cr.getDuration()*1000);  // milliseconds is a good enough precision for us
    }

    @Override
    public String getOutputString(String extension) {
        return className.replace('.','/')+extension;
    }

    @Override
    public String toString() {
        return className +".extension";
    }
}
