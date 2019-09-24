package com.vj.k8s.service.service1;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class IstioService1Application {

	public static void main(String[] args) {
		SpringApplication.run(IstioService1Application.class, args);
	}
	
	@RequestMapping("/hello")
    public String hello() {            
             return String.format("This is hello from version 1");           
    } 
	
	@RequestMapping("/echoserviceheaders")
    public String hello(@RequestHeader Map<String, String> headers) {
		String podName = "Pod Name: "+System.getenv("HOSTNAME")+"\n";
		String mapAsString = headers.entrySet().stream()
				.map(entry -> entry.getKey() + "=" + entry.getValue())
				.collect(Collectors.joining(", \n", "{\n", "\n}\n"));
		String reqDetails = podName + mapAsString;
		return "Service Request Details: \n"+reqDetails;
    }

}
