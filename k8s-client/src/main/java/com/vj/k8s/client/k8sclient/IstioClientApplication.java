package com.vj.k8s.client.k8sclient;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@RestController
public class IstioClientApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(IstioClientApplication.class, args);
	}
	
	@RequestMapping("/welcome")
	public String welcome(@RequestHeader("Authorization") Optional<String> accessToken) {
		String message = "";
		System.out.println("Access Token in Welcome: "+ (accessToken.isEmpty() ? null : accessToken.get()));
		try {
			if(accessToken.isEmpty()) {
				message = new RestTemplate().getForObject("http://k8s-service-1:8081/hello", String.class);
			} else {
				HttpHeaders httpHeaders = new HttpHeaders();
			    httpHeaders.set("Authorization", accessToken.get());
			    HttpEntity<String> httpEntity = new HttpEntity <String> (httpHeaders);
			    ResponseEntity<String> response = new RestTemplate().exchange(
			            "http://k8s-service-1:8081/hello",
			            HttpMethod.GET,
			            httpEntity,
			            String.class);
			    message = response.getBody();
			}
		}catch(Exception e){
			System.out.println("Exception " + e.getMessage());
			message = "Failure";
		}      
		return message;
	}
	
	@RequestMapping("/echoheaders")
	public String echoHeaders(@RequestHeader Map<String, String> headers) {
		String mapAsString = headers.entrySet().stream()
			.map(entry -> entry.getKey() + "=" + entry.getValue())
			.collect(Collectors.joining(", \n", "{\n", "\n}\n"));
		String accessToken = headers.get("Authorization");
		if(null == accessToken) accessToken = headers.get("authorization");
		String serviceMessage = "";
		String returnMessage = "Client Request Details: \n"+mapAsString;
		System.out.println("Access Token in Echo Headers: "+ accessToken);
		try {
			if(null == accessToken) {
				serviceMessage = new RestTemplate().getForObject("http://k8s-service-1:8081/echoserviceheaders", String.class);
			} else {
				HttpHeaders httpHeaders = new HttpHeaders();
			    httpHeaders.set("Authorization", accessToken);
			    HttpEntity<String> httpEntity = new HttpEntity <String> (httpHeaders);
			    ResponseEntity<String> response = new RestTemplate().exchange(
			            "http://k8s-service-1:8081/echoserviceheaders",
			            HttpMethod.GET,
			            httpEntity,
			            String.class);
			    serviceMessage = response.getBody();
			}
		}catch(Exception e){
			System.out.println("Exception " + e.getMessage());
			serviceMessage = "Failure";
		}      
		return returnMessage+serviceMessage;
	}
	
	@RequestMapping("/ping")
	public String ping() {
		return "Ping successfull";
	}

}
