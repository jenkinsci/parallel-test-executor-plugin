# Running

    make run

or to use the uploaded demo:

    docker volume create --name=m2repo
    sudo chmod a+rw $(docker volume inspect -f '{{.Mountpoint}}' m2repo)
    docker run --rm -p 127.0.0.1:8080:8080 -v m2repo:/m2repo -v $(which docker):/usr/bin/docker -v /var/run/docker.sock:/var/run/docker.sock --group-add=$(stat -c %g /var/run/docker.sock) -ti jenkinsci/parallel-test-executor-demo

and then go to: http://localhost:8080/

# Demo contents

`pipeline` is a self-contained Pipeline project.

Run one build of `pipeline » master`—all 400 tests will be run on one slave, which is slow.
Run a second build and you will see the load split across five slaves running 80 tests apiece.

`pipeline-pytest` is a self-contained Pipeline project. It contains a single file containing 100 test cases.

Run one build of `pytest » master`—all 100 test cases in its single test class/file will be run on one slave.
Run a second build and you will see the load split across five slaves running 20 test cases apiece.
