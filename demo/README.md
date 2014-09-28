# Running

    make run

or to use the uploaded demo:

    docker run -p 8080:8080 -ti jenkinsci/parallel-test-executor-demo

and then go to: http://localhost:8080/

Click on _Credentials_ and enter a CloudBees DEV@cloud user account email and password.

# Demo contents

`main` is a freestyle project using the parallel test builder on `sub`.

`flow` is a self-contained workflow project.

[parallel-test-executor-plugin-sample](https://github.com/jenkinsci/parallel-test-executor-plugin-sample/) is the demo project.
