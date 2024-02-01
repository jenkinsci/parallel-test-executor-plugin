package org.jenkinsci.plugins.parallel_test_executor;

import hudson.tasks.junit.ClassResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Execution time of a specific test class.
 */
public class TestClass extends TestEntity {

    final String className;

    public TestClass(ClassResult cr) {
        String pkgName = cr.getParent().getName();
        if (pkgName.equals("(root)"))   // UGH
            pkgName = "";
        else
            pkgName += '.';
        this.className = pkgName+cr.getName();
        this.duration = (long)(cr.getDuration()*1000);  // milliseconds is a good enough precision for us
    }

    //for test estimation for first run
    public TestClass(String className){
        this.className = className;
        this.duration = 10;
    }

    @Override
    public List<String> getOutputString() {
        var sanitizedClassName = className.replace('.', '/');
        return List.of(sanitizedClassName +".java", sanitizedClassName +".class");
    }

    @Override
    public String toString() {
        return className +".extension";
    }
}
