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
* Install minikube and start
```
brew cask install minikube
minikube start --memory=6144 --cpus=2
```
* The below command will build and push the docker image to your docker hub account
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
* The below command will build and push the docker image to your docker hub account
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

# 3) Install Istio
I have installed Istio in Minikube & Docker Desktop on my OSX laptop.
Platform specific instructions are in https://istio.io/docs/setup/kubernetes/platform-setup/
* Create a new kubernetes cluster when using istio.
  * Minikube: Delete and recreate the VM.
    ```
    minikube delete
    minikube start --memory=6144 --cpus=2
    ```
  * Docker Desktop: In 'Docker Desktop' preferences, click 'Reset Kubernetes Cluster'.
    It is recommended to use Docker Desktop because I used it to run all my tests.
    ```
    kubectl config use-context docker-for-desktop
    ```
* Download isto: More details on https://istio.io/docs/setup/kubernetes/#downloading-the-release
```
curl -L https://git.io/getLatestIstio | ISTIO_VERSION=1.2.5 sh -
```
* Set the path to use istioctl
```
cd istio-1.2.5
export PATH=$PWD/bin:$PATH
```
* Setup the cluster to automatically create a istio sidecar for each pod
```
for i in install/kubernetes/helm/istio-init/files/crd*yaml; do kubectl apply -f $i; done
# The below will automatically enable mTLS between all services in a mesh
# If you do not want to enable mTLS meshwide, use command
# kubectl apply -f install/kubernetes/istio-demo.yaml
kubectl apply -f install/kubernetes/istio-demo-auth.yaml
```
* Update the 'default' namespace to automatically enable istio injection. This will automatically add all newly
installed objects with an istio sidecar.
```
# This step can be skipped if you do not want to add sidecar automatically
kubectl label namespace default istio-injection=enabled
```
NOTE: I used ```brew install istioctl``` to install on my mac. Hence, I did not update my PATH variable.
In this case, make sure the istio installed object versions are all the same.
You can verify this by running the below command and validate all versions are 1.2.5
```
 $ istioctl version
client version: 1.2.5
citadel version: 1.2.5
egressgateway version: 1.2.5
galley version: 1.2.5
ingressgateway version: 1.2.5
pilot version: 1.2.5
policy version: 1.2.5
sidecar-injector version: 1.2.5
telemetry version: 1.2.5
```

# 4) Microservice on Kubernetes with Istio mesh in Minikube
* Follow the [Install Istio](#3-install-istio) steps
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

# 5) Run 2 versions of the microservice
The springboot app has a REST endpoint '/hello' that returns a message 'This is hello from version 1'. 
We want to update the message to 'This is hello from version 2'. These 2 should belong to the same microservice,
but to different versions.

* Update 'DOCKER_ACCT' variable in build.sh to the account you have access to.
* Run ```./build.sh``` to create these images.
* Run ```./run.sh``` to run the 2 containers
* Run ```curl http://localhost:8082/hello```. This should return 'This is hello from version 1'
* Run ```curl http://localhost:8083/hello```. This should return 'This is hello from version 2'
* If you are ready, you can run ```./push.sh``` to push the image to the docker account

# 6) Microservice talking to another Microservice
### Prerequisites
Spin up the k8s-service-1 by following these [steps](#2-microservice-on-kubernetes-and-docker-desktop)
### Client Microservice
We write a microservice that will make a call to the k8s-service-1 microservice.

* Enable Kubernetes in Docker Desktop preference
* Switch to docker-for-desktop
```
kubectl config use-context docker-for-desktop
```
* The below command will build and push the docker image to your docker hub account
```
mvn -f k8s-client/pom.xml clean install
```
* Install the service into the default kubernetes cluster
```
kubectl apply -f k8s-client/kubernetes/k8s-client-deployment.yaml
```
* Validate you can access the service. The app is will be listening on port 8081.
The below command gets the NodePort for port 8080
```
curl http://localhost:$(kubectl get services k8s-client-service -o jsonpath='{.spec.ports[?(@.port==8080)].nodePort}')/welcome
```

# 7) Microservice talking to another Microservice with Istio Mesh
* Follow the [Install Istio](#3-install-istio) steps
* Install the 2 microservices
```
# Create  k8s-service-1 deployment and istio ingress gateway
kubectl create -f k8s-service-1/kubernetes/istio-service-1-deployment.yaml
kubectl create -f k8s-service-1/kubernetes/istio-service-1-gateway.yaml

# Create  k8s-client deployment and istio ingress gateway
kubectl create -f k8s-client/kubernetes/istio-client-deployment.yaml
kubectl create -f k8s-client/kubernetes/istio-client-gateway.yaml
```
* Wait for the pods to be created
```
watch kubectl get pods
```
NOTE: Make sure both the service and the sidecar are up. You should see 2/2 to be certain your service is also having a sidecar
* Validate your k8s-service-1 is working as expected
```
curl http://localhost:$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].port}')/hello
```
You should get a string output like
```
This is hello from version 1
```
* Validate your k8s-client microservice is working.
```
curl http://localhost:$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].port}')/welcome
```
You should get a string output like
```
This is hello from version 1
```
This output should be the same as the above because this service calls the k8s-service-1 REST endpoint

# ISTIO capabilities
In the below section, I try to test out the different facets of the istio service mesh

## mTLS between services in a namespace
I would like to enable mTLS authentication for all services in a namespace. In situations where we do not want to enable global mTLS
communication, we would like to be able to create services within a namespace and then secure them with mTLS

* Follow the [Install Istio](#3-install-istio) steps
  * When following the install steps, use ```kubectl apply -f install/kubernetes/istio-demo.yaml``` instead of
    ```kubectl apply -f install/kubernetes/istio-demo-auth.yaml```. This is to prevent meshwide mTLS
  * When following the install steps, do not execute command ```kubectl label namespace default istio-injection=enabled```
* Create a custom namespace called ```vj-istio``` by running the command
  ```
  cd k8s-sandbox
  kubectl apply -f kubernetes-global/create-namespace.yaml
  ```
  Note: The namespace has istio-injection=enabled. This means that all new services will automatically
  have a sidecar created.
  You can remove the automatic creation of a sidecar using the following command
  ```
  kubectl label namespace vj-istio istio-injection-
  kubectl delete pod -n vj-istio $(kubectl get pod -n vj-istio -l app=client -o jsonpath={.items..metadata.name})
  ```
  The pods that are created will not have a sidecar. You can validate that by running command
  ```kubectl get pod -n vj-istio -l app=client```. This will show that only the pod is up without the sidecar.
  You can see READY only shows 1/1 instead of 2/2
  ```
  NAME                            READY   STATUS    RESTARTS   AGE
  k8s-client-7cdfd88958-m8sm6     1/1     Running   0          56s
  ```
* Install the k8s-service-1 and k8s-client microservices
  ```
  kubectl create -f k8s-service-1/kubernetes/istio-service-1-ns-deployment.yaml
  kubectl create -f k8s-service-1/kubernetes/istio-service-1-ns-gateway.yaml
  kubectl create -f k8s-client/kubernetes/istio-client-ns-deployment.yaml
  kubectl create -f k8s-client/kubernetes/istio-client-ns-gateway.yaml
  ```
* Wait for the pods to be created
  ```
  watch kubectl get pods -n vj-istio
  ```
* After the pods are created you can run the below TLS check command
  ```
  istioctl authn tls-check -n vj-istio $(kubectl get pod -n vj-istio -l app=service-1 -o jsonpath={.items..metadata.name}) k8s-service-1.vj-istio.svc.cluster.local
  istioctl authn tls-check -n vj-istio $(kubectl get pod -n vj-istio -l app=client -o jsonpath={.items..metadata.name}) k8s-client.vj-istio.svc.cluster.local
  ```
  If you do not pass in the HOST name at the end of the command, you will see the status of all the services
  ```
  istioctl authn tls-check -n vj-istio $(kubectl get pod -n vj-istio -l app=client -o jsonpath={.items..metadata.name})
  ```
  The output will look like
  ```
  HOST:PORT                                                     STATUS     SERVER        CLIENT     AUTHN POLICY                                 DESTINATION RULE
  k8s-client.vj-istio.svc.cluster.local:8080                    OK         HTTP/mTLS     HTTP       default/                                     -
  k8s-service-1.vj-istio.svc.cluster.local:8081                 OK         HTTP/mTLS     HTTP       default/                                     -
  ```
  As you can see, the server and the client for both services are not using mTLS exclusively
* To validate you can connect to the client and service-1 end points, run the following command
  ```
  # service
  curl http://localhost:80/hello
  # client
  curl http://localhost:80/welcome
  ```
  You should see the output ```This is hello from version 1```
* Apply policy to enable mTLS in the vj-istio namespace
  ```
  kubectl apply -f istio/create-policy.yaml
  ```
* Run the TLS check command
  ```
  istioctl authn tls-check -n vj-istio $(kubectl get pod -n vj-istio -l app=client -o jsonpath={.items..metadata.name})
  ```
  You will notice a CONFLICT
  ```
  HOST:PORT                                                     STATUS       SERVER        CLIENT     AUTHN POLICY                                 DESTINATION RULE
  k8s-client.vj-istio.svc.cluster.local:8080                    CONFLICT     mTLS          HTTP       default/vj-istio                             -
  k8s-service-1.vj-istio.svc.cluster.local:8081                 CONFLICT     mTLS          HTTP       default/vj-istio                             -
  ```
* To validate you can connect to the client and service-1 end points, run the following command
  ```
  # service
  curl http://localhost:80/hello
  ```
  You should see the output ```This is hello from version 1```
  ```
  # client
  curl http://localhost:80/welcome
  ```
  You should see the output ```upstream connect error or disconnect/reset before headers. reset reason: connection termination```.
  This is because the k8s-service-1 sidecar will reject all non-mTLS communucations
* To enable the client to talk with the service, you will need to add a ```DestinationRule```. Run the following command
  ```
  kubectl apply -f istio/create-destination-rule.yaml
  ```
* Run the TLS check command
  ```
  istioctl authn tls-check -n vj-istio $(kubectl get pod -n vj-istio -l app=client -o jsonpath={.items..metadata.name})
  ```
  You will notice the status is now ```OK```
  ```
  HOST:PORT                                                     STATUS     SERVER        CLIENT     AUTHN POLICY                                 DESTINATION RULE
  k8s-client.vj-istio.svc.cluster.local:8080                    OK         mTLS          mTLS       default/vj-istio                             default/vj-istio
  k8s-service-1.vj-istio.svc.cluster.local:8081                 OK         mTLS          mTLS       default/vj-istio                             default/vj-istio
  ```
* To validate you can connect to the client and service-1 end points, run the following command
  ```
  # service
  curl http://localhost:80/hello
  # client
  curl http://localhost:80/welcome
  ```
  You should see the output ```This is hello from version 1```.
* Now all the communication between the services in the namespace are over mTLS

## Origin Authentication using JWT
This is also called end-user authentication. It is used to verify the original service or end-user making the request. We can setup 
Istio to handle origin authentication using JWT. We will use Auth0 for JWT validation.
* Follow the steps mentioned in [mTLS setup](#mtls-between-services-in-a-namespace)
* Create an account with Auth0
  * Click 'APIs' on the left side of the screen and click on 'CREATE API'
    ![Image of Create API Init](images/auth0-api-init.png)
  * If you are running your kubernetes cluster on localhost, enter the following values as shown in the image
    ![Image of Create API](images/auth0-api-create.png)
  * After a successfull creation, you can go to the tab 'Test' and store the curl command
    ![Image of curl command](images/auth0-api-test.png)
  * Click on the 'Applications' on the left side of the screen. You will see an application automatically created.
    ![Image of Application screen](images/auth0-app-init.png)
  * Click on the new application and scroll down and click on the 'Show Advanced Settings' link
    ![Image of Adv. settings](images/auth0-app-adv.png)
  * Click on the 'Endpoints' tab and note the 'JSON Web Key Set' URL. This should be used to ```update istio/create-policy-jwt.yaml``` file.
    ![Image of Endpoints](images/auth0-app-endpoint.png)
* Update the auth policy ```istio/create-policy-jwt.yaml``` using the 'JSON Web Key Set' URL stored from Auth0. 
The fields ```issuer``` and ```jwksUri``` will need to be updated.
* Execute the below command to update the auth policy.
  ```
  kubectl apply -f istio/create-policy-jwt.yaml
  ```
* Validate you have installed ```jq``` in your machine by using command ```jq --version```
* Update your curl command to store the access token in a variable
```
export JWT_TOKEN=$(curl --request POST \
  --url https://vprince.auth0.com/oauth/token \
  --header 'content-type: application/json' \
  --data '{"client_id":"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,"client_secret":"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX","audience":"localhost","grant_type":"client_credentials"}' | jq -r '.access_token')
```
* Validate you have a valid JWT_TOKEN and then test the below command
```
curl http://localhost:80/echoheaders -H "Authorization: Bearer $JWT_TOKEN"
```
* You should get the request headers from both microservices
```
Client Request Details:
{
host=localhost,
user-agent=curl/7.54.0,
accept=*/*,
authorization=Bearer XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,
x-forwarded-for=192.168.65.3,
x-forwarded-proto=http,
x-request-id=f81c7249-c63b-9ec4-b9a4-25074e093929,
content-length=0,
x-envoy-internal=true,
x-forwarded-client-cert=By=spiffe://cluster.local/ns/vj-istio/sa/default;Hash=ebea03cd3c211d2afbd051972b7cc4e75e9c903201af30b197ca7b4d5d8353d7;Subject="";URI=spiffe://cluster.local/ns/istio-system/sa/istio-ingressgateway-service-account,
x-b3-traceid=1b83671f3069eed4413a65fde7f59ac4,
x-b3-spanid=28f43f1f859036ea,
x-b3-parentspanid=413a65fde7f59ac4,
x-b3-sampled=1
}
Service Request Details:
{
host=k8s-service-1:8081,
accept=text/plain, application/json, application/*+json, */*,
authorization=Bearer XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,
user-agent=Java/11.0.4,
x-forwarded-proto=http,
x-request-id=b149ba64-ca36-9539-9e60-65a9a6a3cbe4,
content-length=0,
x-forwarded-client-cert=By=spiffe://cluster.local/ns/vj-istio/sa/default;Hash=03d0b4f4d5d63aed8bcaf7401d3298b76ea0a91577daa75e53de3fdc353dfad1;Subject="";URI=spiffe://cluster.local/ns/vj-istio/sa/default,
x-b3-traceid=bfe2b930b0e3360fce4688d634bc85c6,
x-b3-spanid=e0e990802da0c26c,
x-b3-parentspanid=ce4688d634bc85c6,
x-b3-sampled=1
}
```

## RBAC for microservices
In this section we will use Istio's RBAC (Role Based Access Control) to control service access

* Follow the steps mentioned in [Origin Auth using JWT](#origin-authentication-using-jwt)
* Run the following command to restrict access to the ```k8s-service-1``` service from only the ```k8s-client``` service
  ```
  kubectl apply -f istio/create-service-role.yaml
  ```
* The following commands will only work. Note the changes to the ```version``` header values in the commands below.
  ```
  curl http://localhost:80/welcome -H "Authorization: Bearer $JWT_TOKEN" -H "version: v1"
  curl http://localhost:80/welcome -H "Authorization: Bearer $JWT_TOKEN" -H "version: v2"
  curl http://localhost:80/echoheaders -H "Authorization: Bearer $JWT_TOKEN" -H "version: v3"
  curl http://localhost:80/echoheaders -H "Authorization: Bearer $JWT_TOKEN" -H "version: v4"
  ```
  In addition to restricting access to a service using user name (in this case, 'service user name'), access can be
  restricted based on header information. The ```welcome``` endpoint only allows access to requests with header
  ```v1``` and ```v2```.
* The following commands will not work. Note that we are trying to directly access the ```k8s-service-1``` service.
  ```
  # We are trying to directly access the endpoint in 'k8s-service-1'.
  curl http://localhost:80/hello -H "Authorization: Bearer $JWT_TOKEN"
  # We are not passing the correct versions 'v1' and 'v2' for the 'version' value
  curl http://localhost:80/welcome -H "Authorization: Bearer $JWT_TOKEN" -H "version: v3"
  ```
  The output for access failure will be
  ```
  RBAC: access denied
  ```
