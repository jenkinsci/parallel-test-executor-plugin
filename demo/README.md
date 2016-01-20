# Running

    make run

or to use the uploaded demo:

    docker run -p 8080:8080 -ti jenkinsci/parallel-test-executor-demo

and then go to: http://localhost:8080/

# Demo contents

`main` is a freestyle project using the parallel test builder on `sub`.

`pipeline` is a self-contained Pipeline project.
