package com.nabil.teranga_pay_api;

import org.springframework.boot.SpringApplication;

public class TestTerangaPayApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(TerangaPayApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
