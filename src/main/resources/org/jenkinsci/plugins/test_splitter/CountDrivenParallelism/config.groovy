package org.jenkinsci.plugins.test_splitter.CountDrivenParallelism

def f = namespace(lib.FormTagLib)

f.entry(title:"# of parallel tests", field:"size") {
    f.number()
}
