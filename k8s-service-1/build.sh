#!/bin/bash
# update the varible to use the docker account to push
DOCKER_ACCT="vprince"

mvn clean package
# push to docker hub as version 1.0
docker build -t $DOCKER_ACCT/istio-service:1.0 .

# update the response to from 'version 1' to 'version 2'
gsed -i 's/version 1/version 2/g' ./src/main/java/com/vj/k8s/service/service1/IstioService1Application.java
mvn clean package

# push to docker hub as version 2.0
docker build -t $DOCKER_ACCT/istio-service:2.0 .

# Revert the outoput string
gsed -i 's/version 2/version 1/g' ./src/main/java/com/vj/k8s/service/service1/IstioService1Application.java
mvn clean package
