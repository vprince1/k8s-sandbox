package com.vj.k8s.service.service1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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

}
