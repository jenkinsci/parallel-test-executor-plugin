# Prerequisites

* docker
* docker-compose

# Running

    make run

and then go to: http://localhost:8080/

# Tear down

    make clean

# Demo contents

`pipeline` is a self-contained Pipeline project.

Run one build of `pipeline » main` — Jenkins will attempt to guess how to split 100 tests across agents.
Run a second build and you will see the load split more reliably across five agents running ~20 tests apiece.
