FROM jenkins/jenkins:2.319.3

USER root

ENV MAVEN_VERSION=3.8.4
RUN curl -s https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xvfCz - /opt && \
    ln -sv /opt/apache-maven-$MAVEN_VERSION/bin/mvn /usr/local/bin/mvn

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
ENV JAVA_OPTS -Dhudson.model.LoadStatistics.clock=1000 -Dhudson.Main.development=true

ADD JENKINS_HOME /usr/share/jenkins/ref
