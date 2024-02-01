package org.jenkinsci.plugins.parallel_test_executor;

import hudson.tasks.junit.CaseResult;
import java.util.List;

/**
 * Execution time of a specific test case.
 */
public class TestCase extends TestEntity {
    String output;
    
    public TestCase(CaseResult cr, boolean withClassName) {
        if (withClassName) {
            this.output = cr.getFullName();
        } else {
            this.output = cr.getName();
        }
        this.duration = (long)(cr.getDuration()*1000);  // milliseconds is a good enough precision for us
    }

    @Override
    public List<String> getOutputString() {
        return List.of(output);
    }
    
    @Override
    public String toString() {
        return output;
    }
}
