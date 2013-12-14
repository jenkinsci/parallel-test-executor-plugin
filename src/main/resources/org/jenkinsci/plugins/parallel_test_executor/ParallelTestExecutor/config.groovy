package org.jenkinsci.plugins.parallel_test_executor.ParallelTestExecutor

import jenkins.model.Jenkins
import org.jenkinsci.plugins.parallel_test_executor.Parallelism;

def f = namespace(lib.FormTagLib)

f.entry(title:"Test job to run", field:"testJob") {
    f.textbox()
}
f.entry(title:"Exclusion file name in the test job", field:"patternFile") {
    f.textbox()
}
f.entry(title:"Degree of parallelism", field:"parallelism") {
    f.hetero_radio(field:"parallelism", descriptors:Jenkins.instance.getDescriptorList(Parallelism.class))
}
f.entry(title:"Test report directory in the test job", field:"testReportFiles") {
    f.textbox()
}
f.advanced {
    f.entry(title:"Automatically archive JUnit test results", field:"archiveTestResults") {
        f.checkbox(default: true)
    }
}
