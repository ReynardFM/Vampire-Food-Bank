package com.project.BloodBank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// The entry point. Everything starts here.
//
// @SpringBootApplication is three annotations in one: it marks this as a configuration class, turns
// on auto-configuration (which is what wires up the web server, Hibernate and Spring Security from
// what it finds on the classpath), and starts a component scan from this package downwards.
//
// That last part is why this class sits above controller, service, repository and the rest -
// anything outside com.project.BloodBank would never be found.
@SpringBootApplication
public class BloodBankApplication {

	public static void main(String[] args) {
		// Starts the container, builds every bean, and runs an embedded Tomcat. It does not return
		// until the application shuts down.
		SpringApplication.run(BloodBankApplication.class, args);
	}

}
