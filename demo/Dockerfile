FROM jenkins/jenkins:2.375.2

USER root

ENV MAVEN_VERSION=3.8.7
RUN curl -s https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xvfCz - /opt && \
    ln -sv /opt/apache-maven-$MAVEN_VERSION/bin/mvn /usr/local/bin/mvn

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

HEALTHCHECK --interval=5m --timeout=3s \
  CMD curl -f http://localhost:8080/ || exit 1
