package org.jenkinsci.plugins.test_splitter.ParallelTestExecutor

import jenkins.model.Jenkins
import org.jenkinsci.plugins.test_splitter.Parallelism;

def f = namespace(lib.FormTagLib)

f.entry(title:"Test job to run",field:"testJob") {
    f.textbox()
}
f.entry(title:"Exclusion file name in the test job",field:"patternFile") {
    f.textbox()
}
f.entry(title:"Degree of parallelism", field:"parallelism") {
    f.hetero_radio(field:"parallelism", descriptors:Jenkins.instance.getDescriptorList(Parallelism.class))
}
f.entry(title:"Test report directory in the test job",field:"testReportFiles") {
    f.textbox()
}
