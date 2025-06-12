#!/bin/bash

MVN_REPO=~/.m2/repository

if [ $# -ge 1 ]; then
        MVN_REPO=${1}
else
	:;
fi

CURRENT_REVISION=$(grep -Po '<revision>[^<]+<\/revision>' pom.xml | sed 's/<revision>\([^>]\+\)<\/revision>/\1/')

echo "Using MVN_REPO=${MVN_REPO}, CURRENT_REVISION=${CURRENT_REVISION}"

mvn -e -Dmaven.repo.local=${MVN_REPO} clean install -Dmaven.test.skip=true && #  Only required when dependency packages have been updated.
mvn -e -Dmaven.repo.local=${MVN_REPO} -pl res-modules/block-mining-simulation-game-core -amd clean install  -Dmaven.test.skip=true &&
mvn -e -Dmaven.repo.local=${MVN_REPO} -pl res-modules/block-mining-simulation-game-single-player-client -amd clean compile package spring-boot:repackage -Dmaven.test.skip=true 


#  Example of how to run jar so that a profiler can be attached:
#  java -Dspring-boot.run.jvmArguments="-Xdebug -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.rmi.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=localhost" -jar res-modules/block-mining-simulation-game-single-player-client/target/block-mining-simulation-game-single-player-client-0.0.7.jar --log-file /tmp/single-player-block-client.log --block-world-file /tmp/single-player-world.sqlite

#  If build was successful
if [ $? -eq 0 ]; then
	#  If there's a second param, just copy the jar for release:
	if [ $# -ge 2 ]; then
		cd res-modules/block-mining-simulation-game-single-player-client/target/ && sha512sum block-mining-simulation-game-single-player-client-${CURRENT_REVISION}.jar > block-mining-simulation-game-single-player-client-${CURRENT_REVISION}.jar.sha512
	#  Otherwise, run the game:
	else
		#  Just run the game
		java -jar res-modules/block-mining-simulation-game-single-player-client/target/block-mining-simulation-game-single-player-client-${CURRENT_REVISION}.jar --log-file /tmp/single-player-block-client.log --block-world-file /tmp/single-player-world.sqlite
	fi
fi
