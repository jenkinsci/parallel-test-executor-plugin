package org.jenkinsci.plugins.test_splitter.TimeDrivenParallelism

def f = namespace(lib.FormTagLib)

f.entry(title:"Minutes per execution", field:"mins") {
    f.number()
}
