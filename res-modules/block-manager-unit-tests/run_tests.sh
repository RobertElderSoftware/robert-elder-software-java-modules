#!/bin/bash

mvn clean install && #  Only required when dependency packages have been updated.
mvn -pl res-modules/block-manager-core -amd clean install &&
mvn -pl res-modules/block-manager-unit-tests -amd clean compile package spring-boot:repackage &&
java -jar res-modules/block-manager-unit-tests/target/block-manager-unit-tests-0.0.2.jar
