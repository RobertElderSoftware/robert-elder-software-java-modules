#!/bin/bash

mvn clean install -Dmaven.test.skip=true && #  Only required when dependency packages have been updated.
mvn -pl res-modules/block-mining-simulation-game-multi-player-client -amd clean compile package spring-boot:repackage -Dmaven.test.skip=true &&
java -jar res-modules/block-mining-simulation-game-multi-player-client/target/final-block-mining-simulation-game-multi-player-client.jar
