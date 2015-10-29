package org.jenkinsci.plugins.parallel_test_executor;

public enum TestCaseCentricFormat  {
    DISABLED ("Disabled"),
    TCNAME ("Enabled - testCaseName"),
    CLNAME_DOT_TCNAME ("Enabled - className.testCaseName");

    private final String description;

    TestCaseCentricFormat(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }

    static public boolean isEnabled(TestCaseCentricFormat format) {
        return (format != null) && (format != TestCaseCentricFormat.DISABLED);
    }
}
