FROM jenkins/jenkins:2.150.1

USER root

ENV DOCKER_BUCKET get.docker.com
ENV DOCKER_VERSION 1.12.6
RUN set -x \
    && curl -fSL "https://${DOCKER_BUCKET}/builds/Linux/x86_64/docker-$DOCKER_VERSION.tgz" -o docker.tgz \
    && tar -xzvf docker.tgz \
    && mv docker/* /usr/local/bin/ \
    && rmdir docker \
    && rm docker.tgz \
    && docker -v

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
ENV JAVA_OPTS -Dhudson.model.LoadStatistics.clock=1000 -Dhudson.DNSMultiCast.disabled=true -Dhudson.Main.development=true

ADD JENKINS_HOME /usr/share/jenkins/ref
