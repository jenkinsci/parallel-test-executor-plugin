package org.jenkinsci.plugins.parallel_test_executor;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * A list of file name patterns to include or exclude
 */
public class InclusionExclusionPattern implements Serializable {
    @Whitelisted
    public boolean isIncludes() {
        return includes;
    }

    @Whitelisted
    public List<String> getList() {
        return Collections.unmodifiableList(list);
    }

    private final boolean includes;
    private final List<String> list;

    public InclusionExclusionPattern(List<String> list, boolean includes) {
        this.list = list;
        this.includes = includes;
    }

    @Override
    public String toString() {
        return "InclusionExclusionPattern{" +
                "includes=" + includes +
                ", list=" + list +
                '}';
    }
}
