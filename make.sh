#!/usr/bin/env bash
#__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
#

if [ "$1" = "prepare-core" ]; then
	echo "Search-enabler: Preparing core"
	rm -rf /tmp/orchestrator && git clone https://orchestrator:weRm4nhQcjTyacFuPbLk@gitlab.iotcrawler.net/orchestrator/orchestrator.git tmp/orchestrator
	export CURR=$(pwd) && cd /tmp/orchestrator
	sed -i 's/<phase>process-sources<\/phase>/<phase>none<\/phase>/' IoTCrawler/pom.xml
	sh make.sh install && cd ${CURR}
fi

if [ "$1" = "package" ]; then
	echo "Search enabler: Checking core dependency"
	(if [ ! -d ~/.m2/repository/com/agtinternational/iotcrawler/core ]; then sh make.sh prepare-core; fi);
	mvn clean package -DskipTests=true
fi

if [ "$1" = "build-image" ]; then
   echo "Search enabler: Checking core dependency"
   (if [ ! -d ~/.m2/repository/com/agtinternational/iotcrawler/core ]; then sh make.sh prepare-core; fi);
	 mvn clean package -DskipTests=true jib:dockerBuild -U
fi

if [ "$1" = "push-image" ]; then
#  echo "# Setting env vars for pushing"
  if [ -z "$CI_COMMIT_TAG" ]; then
        export CI_APPLICATION_REPOSITORY=${CI_APPLICATION_REPOSITORY:-$CI_REGISTRY_IMAGE/$CI_COMMIT_REF_SLUG}
        export CI_APPLICATION_TAG=${CI_APPLICATION_TAG:-$CI_COMMIT_SHA}
      else
        export CI_APPLICATION_REPOSITORY=${CI_APPLICATION_REPOSITORY:-$CI_REGISTRY_IMAGE}
        export CI_APPLICATION_TAG=${CI_APPLICATION_TAG:-$CI_COMMIT_TAG}
  fi

# gitlab.iotcrawler.net:4567/orchestrator/orchestrator is already in variables (on in a gitlab)
  echo "# docker tag ${CI_APPLICATION_REPOSITORY}:${CI_APPLICATION_TAG} ${CI_APPLICATION_TAG}"
  docker tag ${CI_APPLICATION_REPOSITORY}:${CI_APPLICATION_TAG} ${CI_APPLICATION_TAG}
  echo "# docker push ${CI_APPLICATION_REPOSITORY}:$CI_APPLICATION_TAG"
  docker push ${CI_APPLICATION_REPOSITORY}:$CI_APPLICATION_TAG
	#docker push "${CI_APPLICATION_REPOSITORY}:latest"
fi
