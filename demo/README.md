# Running

    make run

and then go to: http://localhost:8080/

# Demo contents

`pipeline` is a self-contained Pipeline project.

Run one build of `pipeline » master`—Jenkins will attempt to guess how to split 400 tests across agents.
Run a second build and you will see the load split more reliably across five slaves running 80 tests apiece.
