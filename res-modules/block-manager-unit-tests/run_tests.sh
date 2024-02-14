#!/bin/bash

MVN_REPO=~/.m2/repository

if [ $# -ge 1 ]; then
        MVN_REPO=${1}
else
	:;
fi

CURRENT_REVISION=$(grep -Po '<revision>[^<]+<\/revision>' pom.xml | sed 's/<revision>\([^>]\+\)<\/revision>/\1/')

mvn -e -Dmaven.repo.local=${MVN_REPO} clean install && #  Only required when dependency packages have been updated.
mvn -e -Dmaven.repo.local=${MVN_REPO} -pl res-modules/block-manager-core -amd clean install &&
mvn -e -Dmaven.repo.local=${MVN_REPO} -pl res-modules/block-manager-unit-tests -amd clean compile package spring-boot:repackage &&
java -jar res-modules/block-manager-unit-tests/target/block-manager-unit-tests-${CURRENT_REVISION}.jar
