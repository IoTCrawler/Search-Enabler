#!/usr/bin/env bash
#__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
#


if [ "$1" = "prepare-fiware-models" ]; then
	#Fiware/clients: Preparing iot-broker
	(if [ ! -d /tmp/iotc-fiware-models ]; then git clone git@gitlab.iotcrawler.net:core/fiware-models.git /tmp/iotc-fiware-models ; fi);
	cd /tmp/iotc-fiware-models && mvn install -DskipTests=true
fi

if [ "$1" = "prepare-core-models" ]; then
	#Fiware/clients: Preparing iot-broker
	(if [ ! -d /tmp/iotc-core-models ]; then git clone git@gitlab.iotcrawler.net:core/core-models.git /tmp/iotc-core-models ; fi);
	cd /tmp/iotc-core-models && mvn install -DskipTests=true
fi


if [ "$1" = "install" ]; then
	#Fiware/clients: Checking fiware-models dependency
	(if [ ! -d ~/.m2/repository/com/agtinternational/iotcrawler/fiware-models ]; then sh make.sh prepare-fiware-models; fi);
	#Fiware/clients: Checking core-models dependency
	(if [ ! -d ~/.m2/repository/com/agtinternational/iotcrawler/core-models ]; then sh make.sh prepare-core-models; fi);
	#Fiware/clients: installing
	mvn install -DskipTests=true
fi
