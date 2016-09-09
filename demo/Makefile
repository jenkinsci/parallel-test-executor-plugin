TAG=$(shell perl -n -e 'if (/parallel-test-executor:(.+)/) {print $$1}' plugins.txt)
IMAGE=jenkinsci/parallel-test-executor-demo
DOCKER_RUN=docker run --rm -p 8080:8080 -v m2repo:/m2repo -v /var/run/docker.sock:/var/run/docker.sock --group-add=$(shell stat -c %g /var/run/docker.sock) -ti

build:
	docker build -t $(IMAGE):$(TAG) .

# http://stackoverflow.com/q/23544282/12916 unclear how best to ensure that the jenkins user can write to this volume
volume:
	docker volume create --name=m2repo
	sudo chmod a+rw $$(docker volume inspect -f '{{.Mountpoint}}' m2repo)

run: build volume
	$(DOCKER_RUN) $(IMAGE):$(TAG)

build-snapshot:
	docker build -t $(IMAGE):RELEASE .
	[ -f ../target/parallel-test-executor.hpi ] || mvn -f .. -DskipTests clean install
	cp -v ../target/parallel-test-executor.hpi snapshot/parallel-test-executor.jpi
	docker build -t $(IMAGE):SNAPSHOT snapshot

run-snapshot: build-snapshot volume
	$(DOCKER_RUN) $(IMAGE):SNAPSHOT

clean:
	rm -rf snapshot/plugins

push:
	docker push $(IMAGE):$(TAG)
	echo "consider also: make push-latest"

push-latest: push
	docker tag $(IMAGE):$(TAG) $(IMAGE):latest
	docker push $(IMAGE):latest
