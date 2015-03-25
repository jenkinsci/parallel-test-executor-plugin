FROM java:7

ADD JENKINS_HOME /var/lib/jenkins/
ADD jenkins-war.war /var/lib/jenkins/jenkins.war

ADD run.sh /var/lib/jenkins/run.sh

CMD /var/lib/jenkins/run.sh
EXPOSE 8080
