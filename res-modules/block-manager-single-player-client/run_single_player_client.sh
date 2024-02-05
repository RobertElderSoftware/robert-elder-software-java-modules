#!/bin/bash

# To install maven dependencies in a throw-away repo instead of the default one, include:
# -e -Dmaven.repo.local=/tmp/tmp123-mvn-repo

mvn clean install && #  Only required when dependency packages have been updated.
mvn -pl res-modules/block-manager-core -amd clean install &&
mvn -pl res-modules/block-manager-single-player-client -amd clean compile package spring-boot:repackage &&
java -jar res-modules/block-manager-single-player-client/target/block-manager-single-player-client-0.0.1.jar --log-file /tmp/single-player-block-client.log --block-world-file /tmp/single-player-world.sqlite
