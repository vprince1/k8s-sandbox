#!/bin/bash
docker run --name k8s-client \
	   -p 8080:8080 \
           -d k8s-client:1.0.0
