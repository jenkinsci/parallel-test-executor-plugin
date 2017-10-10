TAG=$(shell date -I -u)
IMAGE=jenkinsci/parallel-test-executor-demo
DOCKER_RUN=docker run --rm -p 127.0.0.1:8080:8080 -v m2repo:/m2repo -v $(shell which docker):/usr/bin/docker -v /var/run/docker.sock:/var/run/docker.sock --group-add=$(shell stat -c %g /var/run/docker.sock) -ti

copy-plugins:
	if [ \! -f ../target/parallel-test-executor.hpi ]; then mvn -f .. -DskipTests install; fi
	if [ \! -f target/test-classes/test-dependencies/index -o \
	     pom.xml -nt target/test-classes/test-dependencies/index -o \
	     ../target/parallel-test-executor.hpi -nt target/test-classes/test-dependencies/parallel-test-executor.hpi ]; then \
	    mvn clean validate hpi:resolve-test-dependencies; fi
	# TODO would be more efficient to move Dockerfile and all it references into a subdirectory, since docker-build lacks an --excludes
	rm -rf plugins
	mkdir plugins
	cp -v target/test-classes/test-dependencies/*.hpi plugins

build: copy-plugins
	docker build -t $(IMAGE):$(TAG) .

# http://stackoverflow.com/q/23544282/12916 unclear how best to ensure that the jenkins user can write to this volume
volume:
	docker volume create --name=m2repo
	sudo chmod a+rw $$(docker volume inspect -f '{{.Mountpoint}}' m2repo)

run: build volume
	$(DOCKER_RUN) $(IMAGE):$(TAG)

push:
	docker push $(IMAGE):$(TAG)
	echo "consider also: make push-latest"

push-latest: push
	docker tag $(IMAGE):$(TAG) $(IMAGE):latest
	docker push $(IMAGE):latest
