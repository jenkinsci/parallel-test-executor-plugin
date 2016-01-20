# Running

    make run

or to use the uploaded demo:

    docker run -p 8080:8080 -ti jenkinsci/parallel-test-executor-demo

and then go to: http://localhost:8080/

# Demo contents

`main` is a freestyle project using the parallel test builder on `sub`.

`pipeline` is a self-contained Pipeline project.

Run one build of `main` or `pipeline`—all 400 tests will be run on one slave, which is slow.
Run a second build and you will see the load split across five slaves running 80 tests apiece.
