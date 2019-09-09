#!/bin/bash
docker run --name istio-service-1 \
	   -p 8082:8081 \
           -d vprince/istio-service:1.0
docker run --name istio-service-2 \
	   -p 8083:8081 \
           -d vprince/istio-service:2.0
