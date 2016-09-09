FROM jenkins:2.7.3

USER root

RUN wget -q -O /usr/bin/docker --no-check-certificate https://get.docker.com/builds/Linux/x86_64/docker-1.9.0 && \
    chmod a+x /usr/bin/docker

ADD repo /tmp/repo
COPY gen.sh /tmp/
ADD lib /tmp/lib
COPY plugins /usr/share/jenkins/ref/plugins
RUN chown -R jenkins.jenkins /tmp/repo /tmp/lib /usr/share/jenkins/ref/plugins

USER jenkins

RUN cd /tmp/repo && \
    bash ../gen.sh && \
    git init && \
    git add . && \
    git -c user.email=demo@jenkins-ci.org -c user.name="Parallel Test Executor Demo" commit -m 'demo' && \
    cd /tmp/lib && \
    git init && \
    git add . && \
    git -c user.email=demo@jenkins-ci.org -c user.name="Parallel Test Executor Demo" commit -m 'demo'

# TODO without this JENKINS-24752 workaround, it takes too long to provision.
# (Do not add hudson.model.LoadStatistics.decay=0.1; in that case we overprovision slaves which never get used, and OnceRetentionStrategy.check disconnects them after an idle timeout.)
ENV JAVA_OPTS -Dhudson.model.LoadStatistics.clock=1000

ADD JENKINS_HOME /usr/share/jenkins/ref
