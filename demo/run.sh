#!/bin/bash
JENKINS_HOME=/var/lib/jenkins java -Djava.security.egd=file:/dev/./urandom -Dhudson.model.LoadStatistics.decay=0.1 -Dhudson.model.LoadStatistics.clock=1000 -jar /var/lib/jenkins/jenkins.war
