package org.jenkinsci.plugins.parallel_test_executor.TimeDrivenParallelism

def f = namespace(lib.FormTagLib)

f.entry(title:"Minutes per execution", field:"mins") {
    f.number()
}
