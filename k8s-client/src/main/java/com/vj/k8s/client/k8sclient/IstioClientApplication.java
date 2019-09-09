package com.vj.k8s.client.k8sclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
	public String welcome() {
		String message = "";
		try {
            message = new RestTemplate().getForObject("http://k8s-service-1:8081/hello", String.class);
		}catch(Exception e){
			System.out.println("Exception " + e.getMessage());
			message = "Failure";
		}      
		return message;
	}
	
	@RequestMapping("/ping")
	public String ping() {
		return "Ping successfull";
	}

}
