#!/bin/bash

STAGE=abc
PROFILE=abc
MVN=/mnt/apache-maven-3.9.6/bin/mvn

if [ $# -ne 1 ]
then
        echo "Wrong number of arguments:  Specify the stage."
        exit 1
else
        if [ "${1}" = "dev" ]; then
                STAGE=tomcat9-block-manager
                PROFILE=development
        else
                STAGE=tomcat9-block-manager
                PROFILE=development
        fi
fi

echo "Using stage=${STAGE} profile=${PROFILE}."

#  This is only needed if changes are made to dependencies:
$MVN -P ${PROFILE} clean install -Dmaven.test.skip=true &&
$MVN -pl res-modules/block-mining-simulation-game-core -amd clean install -Dmaven.test.skip=true && $MVN -P ${PROFILE} -pl res-modules/block-mining-simulation-game-server -amd clean compile package spring-boot:repackage
sudo cp res-modules/block-mining-simulation-game-server/target/final-block-mining-simulation-game-server.war /mnt/$STAGE/webapps/ROOT.war &&
sudo service $STAGE restart
