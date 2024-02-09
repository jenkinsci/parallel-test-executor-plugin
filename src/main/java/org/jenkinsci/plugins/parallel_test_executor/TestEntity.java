package org.jenkinsci.plugins.parallel_test_executor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jenkinsci.plugins.parallel_test_executor.ParallelTestExecutor.Knapsack;

/**
 * Represents a result of the test parallelization granularity of interest.
 */
@SuppressFBWarnings(value="EQ_COMPARETO_USE_OBJECT_EQUALS", justification="Cf. justification in Knapsack.")
public abstract class TestEntity implements Comparable<TestEntity> {

    long duration;
    /**
     * Knapsack that this test class belongs to.
     */
    Knapsack knapsack;

    TestEntity() {}

    public long getDuration() {
        return duration;
    }

    @Override
    public int compareTo(TestEntity that) {
        long l = this.duration - that.duration;
        // sort them in the descending order
        if (l>0)    return -1;
        if (l<0)    return 1;
        return 0;
    }

    public abstract String getKey();
    
    public abstract List<String> getElements();
}
