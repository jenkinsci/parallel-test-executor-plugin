package org.jenkinsci.plugins.test_splitter.TimeDrivenParallelism

def f = namespace(lib.FormTagLib)

f.entry(title:"# of test minutes per execution", field:"mins") {
    f.number()
}
