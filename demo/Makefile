TAG=1.8
IMAGE=jenkinsci/parallel-test-executor-demo
DOCKER_RUN=docker run --rm -p 8080:8080 -ti

build:
	docker build -t $(IMAGE):$(TAG) .

run: build
	$(DOCKER_RUN) $(IMAGE):$(TAG)

build-snapshot:
	docker build -t $(IMAGE):RELEASE .
	[ -f ../target/parallel-test-executor.hpi ] || mvn -f .. -DskipTests clean install
	cp -v ../target/parallel-test-executor.hpi snapshot/parallel-test-executor.jpi
	docker build -t $(IMAGE):SNAPSHOT snapshot

run-snapshot: build-snapshot
	$(DOCKER_RUN) $(IMAGE):SNAPSHOT

clean:
	rm -rf snapshot/plugins

push:
	docker push $(IMAGE):$(TAG)
	echo "consider also: make push-latest"

push-latest: push
	docker tag -f $(IMAGE):$(TAG) $(IMAGE):latest
	docker push $(IMAGE):latest
