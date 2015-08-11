package org.jenkinsci.plugins.parallel_test_executor;

import java.util.Collections;
import java.util.List;

/**
 * @author Vincent Latombe <vincent@latombe.net>
 */
public class InclusionExclusionPattern {
    public boolean isIncludes() {
        return includes;
    }

    public List<String> getList() {
        return Collections.unmodifiableList(list);
    }

    private final boolean includes;
    private final List<String> list;

    InclusionExclusionPattern(List<String> list, boolean includes) {
        this.list = list;
        this.includes = includes;
    }
}
