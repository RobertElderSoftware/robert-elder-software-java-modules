#!/bin/bash

MVN_REPO=~/.m2/repository

if [ $# -ge 1 ]; then
        MVN_REPO=${1}
else
	:;
fi

CURRENT_REVISION=$(grep -Po '<revision>[^<]+<\/revision>' pom.xml | sed 's/<revision>\([^>]\+\)<\/revision>/\1/')

mvn -e -Dmaven.repo.local=${MVN_REPO} clean install && #  Only required when dependency packages have been updated.
mvn -e -Dmaven.repo.local=${MVN_REPO} -pl res-modules/block-mining-simulation-game-core -amd clean install &&
mvn -e -Dmaven.repo.local=${MVN_REPO} -pl res-modules/block-mining-simulation-game-unit-tests -amd test
