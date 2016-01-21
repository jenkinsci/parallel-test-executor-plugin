package org.jenkinsci.plugins.parallel_test_executor;

public enum TestMode  {
    JAVA ("Parallelize on test classes (Java)"),
    TESTCASENAME ("Parallelize on test case name (Generic)"),
    CLASSANDTESTCASENAME ("Parallelize on class and test case name (Generic)");

    private final String description;

    TestMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
