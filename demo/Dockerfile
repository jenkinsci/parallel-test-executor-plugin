FROM jenkins:1.625.3

USER root
ENV MAVEN_VERSION 3.3.1
RUN cd /usr/local; wget -O - http://mirrors.ibiblio.org/apache/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xvzf -
RUN ln -sv /usr/local/apache-maven-$MAVEN_VERSION/bin/mvn /usr/local/bin
USER jenkins

WORKDIR /tmp
ADD repo /tmp/repo-wc-orig
RUN cp -rv repo-wc-orig repo-wc
COPY gen.sh /tmp/
RUN cd repo-wc && bash ../gen.sh
RUN git init --bare repo && cd repo-wc && git init && git add . && git -c user.email=demo@jenkins-ci.org -c user.name="Parallel Test Executor Demo" commit -m 'demo' && git push ../repo master

# Prepare local Maven repo. Note that $JENKINS_HOME is a volume so we cannot populate it now.
RUN echo '<settings><mirrors><mirror><id>central</id><url>http://repo.jenkins-ci.org/simple/repo1-cache/</url><mirrorOf>central</mirrorOf></mirror></mirrors><localRepository>/usr/share/jenkins/ref/.m2/repository</localRepository></settings>' > settings.xml
RUN cd repo-wc && mvn -s /tmp/settings.xml -Dmaven.test.failure.ignore clean install

COPY plugins.txt /tmp/
RUN /usr/local/bin/plugins.sh /tmp/plugins.txt

# TODO without this JENKINS-24752 workaround, it takes too long to provision.
# (Do not add hudson.model.LoadStatistics.decay=0.1; in that case we overprovision slaves which never get used, and OnceRetentionStrategy.check disconnects them after an idle timeout.)
ENV JAVA_OPTS -Dhudson.model.LoadStatistics.clock=1000

ADD JENKINS_HOME /usr/share/jenkins/ref
