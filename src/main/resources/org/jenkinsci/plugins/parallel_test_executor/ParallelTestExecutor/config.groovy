package org.jenkinsci.plugins.parallel_test_executor.ParallelTestExecutor

import jenkins.model.Jenkins
import org.jenkinsci.plugins.parallel_test_executor.Parallelism
import org.jenkinsci.plugins.parallel_test_executor.testmode.TestMode

def f = namespace(lib.FormTagLib)

f.entry(title:"Test job to run", field:"testJob") {
    f.textbox()
}
f.entry(title:"Exclusion file name in the test job", field:"patternFile") {
    f.textbox()
}
f.entry(title:"Optional inclusion file name in the test job", field:"includesPatternFile") {
    f.textbox()
}
f.entry(title:"Degree of parallelism", field:"parallelism") {
    f.hetero_radio(field:"parallelism", descriptors:Jenkins.get().getDescriptorList(Parallelism.class))
}
f.entry(title:"Test parallelism", field:"testMode") {
    f.hetero_radio(field:"testMode", descriptors:Jenkins.get().getDescriptorList(TestMode.class))
}
f.entry(title:"Test report directory in the test job", field:"testReportFiles") {
    f.textbox()
}

f.entry(title:"Estimate tests from file sources", field:"estimateTestsFromFiles") {
    f.checkbox()
}

f.block {
    f.entry(title:'Parameters for test job', help:descriptor.getHelpFile('parameters')) {
        f.repeatableHeteroProperty(
            field:'parameters',
            hasHeader:'true',
            oneEach:"true",
            addCaption:'Add Parameters',
            capture:'configFor')
    }
}

f.advanced {
    f.entry(title:"Automatically archive JUnit test results", field:"archiveTestResults") {
        f.checkbox(default: true)
    }
}
