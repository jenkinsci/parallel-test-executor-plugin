# Running

    make run

or to use the uploaded demo:

    docker volume create --name=m2repo
    sudo chmod a+rw $(docker volume inspect -f '{{.Mountpoint}}' m2repo)
    docker run --rm -p 127.0.0.1:8080:8080 -v m2repo:/m2repo -v /var/run/docker.sock:/var/run/docker.sock --group-add=$(stat -c %g /var/run/docker.sock) -ti jenkinsci/parallel-test-executor-demo

and then go to: http://localhost:8080/

# Demo contents

`pipeline` is a self-contained Pipeline project.

Run one build of `pipeline » master`—Jenkins will attempt to guess how to split 400 tests across agents.
Run a second build and you will see the load split more reliably across five slaves running 80 tests apiece.
