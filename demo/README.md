# Prerequisites

* docker
* docker compose

# Running

    make run

and then go to: http://localhost:8080/

# Tear down

    make clean

# Demo contents

`pipeline` is a self-contained Pipeline project.

Run one build of `pipeline » main` — Jenkins will attempt to guess how to split 100 tests across agents.
Run a second build and you will see the load split more reliably across five agents running ~20 tests apiece.

`pipeline-pytest` is a self-contained Pipeline project. It contains a single file containing 100 test cases.

Run one build of `pytest » master`—all 100 test cases in its single test class/file will be run on one agent.
Run a second build and you will see the load split across five agents running 20 test cases apiece.
