FROM maven:3.9.6-eclipse-temurin-11 AS maven
FROM jenkins/jenkins:2.387.3

USER root

RUN apt-get update &&  \
    apt-get install -y python3-pytest && \
    apt-get clean

COPY --from=maven /usr/share/maven /usr/share/maven/
RUN ln -s /usr/share/maven/bin/mvn /usr/local/bin/mvn && \
    ln -s /usr/share/maven/bin/mvnDebug /usr/local/bin/mvnDebug

ADD lib /tmp/lib
RUN mkdir -p /m2repo

RUN chown -R jenkins.jenkins /tmp/lib /m2repo

USER jenkins

COPY target/test-classes/test-dependencies/*.hpi /usr/share/jenkins/ref/plugins/

RUN cd /tmp/lib && \
    git init && \
    git add . && \
    git -c user.email=demo@jenkins-ci.org -c user.name="Parallel Test Executor Demo" commit -m 'demo'

# TODO without this JENKINS-24752 workaround, it takes too long to provision.
# (Do not add hudson.model.LoadStatistics.decay=0.1; in that case we overprovision slaves which never get used, and OnceRetentionStrategy.check disconnects them after an idle timeout.)
ENV JAVA_OPTS -Dhudson.model.LoadStatistics.clock=1000 -Dhudson.Main.development=true -Dhudson.plugins.git.GitSCM.ALLOW_LOCAL_CHECKOUT=true

ADD JENKINS_HOME /usr/share/jenkins/ref
