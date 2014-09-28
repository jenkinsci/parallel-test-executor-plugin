TAG=jenkinsci/parallel-test-executor-demo

build: releases
	docker build -t $(TAG) .

run: build
	docker run -p 8080:8080 -ti $(TAG)

PLUGINS=\
	com.cloudbees.jenkins.plugins:cloudbees-credentials \
	com.cloudbees.jenkins.plugins:cloudbees-registration \
	com.cloudbees.jenkins.plugins:mansion-cloud \
	org.jenkins-ci.plugins.workflow:workflow-aggregator \
	org.jenkins-ci.plugins.workflow:workflow-api \
	org.jenkins-ci.plugins.workflow:workflow-basic-steps \
	org.jenkins-ci.plugins.workflow:workflow-cps \
	org.jenkins-ci.plugins.workflow:workflow-durable-task-step \
	org.jenkins-ci.plugins.workflow:workflow-job \
	org.jenkins-ci.plugins.workflow:workflow-scm-step \
	org.jenkins-ci.plugins.workflow:workflow-step-api \
	org.jenkins-ci.plugins.workflow:workflow-stm \
	org.jenkins-ci.plugins.workflow:workflow-support \
	org.jenkins-ci.plugins:async-http-client \
	org.jenkins-ci.plugins:credentials \
	org.jenkins-ci.plugins:durable-task \
	org.jenkins-ci.plugins:git \
	org.jenkins-ci.plugins:git-client \
	org.jenkins-ci.plugins:junit \
	org.jenkins-ci.plugins:mapdb-api \
	org.jenkins-ci.plugins:parallel-test-executor \
	org.jenkins-ci.plugins:parameterized-trigger \
	org.jenkins-ci.plugins:scm-api \
	org.jenkins-ci.plugins:script-security \
	org.jenkins-ci.plugins:ssh-credentials \
	org.jenkins-ci.plugins:ssh-slaves

releases:
	[ -f jenkins-war.war ] || mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:copy -Dartifact=org.jenkins-ci.main:jenkins-war:RELEASE:war -DoutputDirectory=. -Dmdep.stripVersion
	set -e; for ga in $(PLUGINS); do a=$$(echo $$ga | sed 's/.*\://'); [ -f JENKINS_HOME/plugins/$$a.jpi ] || (mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:copy -Dartifact=$$ga:RELEASE:hpi -DoutputDirectory=JENKINS_HOME/plugins -Dmdep.stripVersion && mv JENKINS_HOME/plugins/$$a.hpi JENKINS_HOME/plugins/$$a.jpi); touch JENKINS_HOME/plugins/$$a.jpi.pinned; done

clean:
	rm -rf jenkins-war.war JENKINS_HOME/plugins

push:
	docker push $(TAG)
