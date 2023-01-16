PHONY: validate
validate:
	mvn -Dtest=InjectedTest clean test

PHONY: copy-plugins
copy-plugins:
	if [ \! -f ../target/parallel-test-executor.hpi ]; then mvn -f .. -Pquick-build install; fi
	if [ \! -f target/test-classes/test-dependencies/index -o \
	     pom.xml -nt target/test-classes/test-dependencies/index -o \
	     ../target/parallel-test-executor.hpi -nt target/test-classes/test-dependencies/parallel-test-executor.hpi ]; then \
	    mvn clean validate hpi:resolve-test-dependencies; fi
	@# TODO would be more efficient to move Dockerfile and all it references into a subdirectory, or use .dockerignore

PHONY: clean
clean:
	rm -rf target
	docker-compose down -v
	docker image rm demo-jenkins || true

PHONY: run
run: copy-plugins
	./run.sh
