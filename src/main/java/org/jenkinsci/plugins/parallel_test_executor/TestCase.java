package org.jenkinsci.plugins.parallel_test_executor;

import hudson.tasks.junit.CaseResult;

/**
 * Execution time of a specific test case.
 */
public class TestCase extends TestEntity {
    String output;
    
    public TestCase(CaseResult cr, TestCaseCentricFormat format) {
        if (format == TestCaseCentricFormat.TCNAME) {
            this.output = cr.getName();
        } else {
            this.output = cr.getFullName();
        }
        this.duration = (long)(cr.getDuration()*1000);  // milliseconds is a good enough precision for us
    }

    @Override
    public String getOutputString(String extension) {
        return output;
    }
    
    @Override
    public String toString() {
        return output;
    }
}
