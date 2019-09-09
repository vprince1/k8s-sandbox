# Description
Create a simple springboot app with a REST endpoint and run it on Kubernetes and also use Istio service mesh.

## Prerequisites for Kubernetes on OSX
* Create a docker hub account
* update ~/.m2/settings.xml
```
<server>
  <id>docker.io</id>
  <username>XXXXX</username>
  <password>YYYYYYYYYYYYY</password>
</server> 
```
* Update docker.image.prefix in pom.xml
* JDK 11

# 1) Microservice on Kubernetes and Minikube

## Running microservice on Minikube and Kubernetes
* Install minikube and start
```
brew cask install minikube
minikube start --memory=6144 --cpus=2
```
* The below command will build and push the docker image to your github account
```
mvn -f k8s-service-1/pom.xml clean install
```
* Install the service into the default kubernetes cluster
```
kubectl apply -f k8s-service-1/kubernetes/k8s-service-1-deployment.yaml
```
* Get the URL of the NodePort
```
minikube service k8s-service-1 --url
```
* Using curl, you can validate the service is up and accessible
```
curl $(minikube service k8s-service-1 --url)/hello
```

# 2) Microservice on Kubernetes and Docker Desktop
We can run kubernetes using Docker Desktop instead of minikube.

* Enable Kubernetes in Docker Desktop preference
* Switch to docker-for-desktop
```
kubectl config use-context docker-for-desktop
```
* The below command will build and push the docker image to your github account
```
mvn -f k8s-service-1/pom.xml clean install
```
* Install the service into the default kubernetes cluster
```
kubectl apply -f k8s-service-1/kubernetes/k8s-service-1-deployment.yaml
```
* Validate you can access the service. The app is will be listening on port 8081.
The below command gets the NodePort for port 8081
```
curl http://localhost:$(kubectl get services k8s-service-1 -o jsonpath='{.spec.ports[?(@.port==8081)].nodePort}')/hello
```

# 3) Microservice on Kubernetes with Istio mesh in Minikube
## Install Istio
I have installed Istio in my local minikube on virtualbox VM in my OSX laptop. Platform specific instructions
are in https://istio.io/docs/setup/kubernetes/platform-setup/

* Start minikube with more than the default memory
```
minikube start --memory=6144 --cpus=2
```
* Install isto: More details on https://istio.io/docs/setup/kubernetes/#downloading-the-release
```
curl -L https://git.io/getLatestIstio | ISTIO_VERSION=1.2.5 sh -
cd istio-1.2.5
export PATH=$PWD/bin:$PATH
```
I used ```brew install istioctl``` to install on my mac.
* Setup the cluster to automatically create a istio sidecar for each pod
```
for i in install/kubernetes/helm/istio-init/files/crd*yaml; do kubectl apply -f $i; done
kubectl apply -f install/kubernetes/istio-demo-auth.yaml
kubectl label namespace default istio-injection=enabled
```
* Install your k8s application
```
kubectl create -f k8s-service-1/kubernetes/istio-service-1-deployment.yaml
```
* Install the istio gateway to access your service
```
kubectl create -f k8s-service-1/kubernetes/istio-service-1-gateway.yaml
```
* Wait for your pod to be ready.
```
watch kubectl get pods
```
You will notice k8s-service-1-* running. You should see 2/2 to be certain your service is also having a sidecar
* To test your service is accessible, run the command
```
export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
export INGRESS_HOST=$(minikube ip)
export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
curl http://$GATEWAY_URL/hello
```
You should get a string output like
```
This is hello from version 1
```

# 4) Run 2 versions of the microservice
The springboot app has a REST endpoint '/hello' that returns a message 'This is hello from version 1'. 
We want to update the message to 'This is hello from version 2'. These 2 should belong to the same microservice,
but to different versions.

* Update 'DOCKER_ACCT' variable in build.sh to the account you have access to.
* Run ```./build.sh``` to create these images.
* Run ```./run.sh``` to run the 2 containers
* Run ```curl http://localhost:8082/hello```. This should return 'This is hello from version 1'
* Run ```curl http://localhost:8083/hello```. This should return 'This is hello from version 2'
* If you are ready, you can run ```./push.sh``` to push the image to the docker account
