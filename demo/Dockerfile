FROM jenkins:2.19.3

USER root

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
ENV JAVA_OPTS -Dhudson.model.LoadStatistics.clock=1000 -Djenkins.install.state=INITIAL_SECURITY_SETUP

ADD JENKINS_HOME /usr/share/jenkins/ref
