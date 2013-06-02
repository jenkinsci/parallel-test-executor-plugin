package org.jenkinsci.plugins.test_splitter.TestSplitter

import jenkins.model.Jenkins
import org.jenkinsci.plugins.test_splitter.Parallelism;

def f = namespace(lib.FormTagLib)

f.entry(title:"Degree of Parallelism", field:"parallelism") {
    f.hetero_radio(field:"parallelism", descriptors:Jenkins.instance.getDescriptorList(Parallelism.class))
}
