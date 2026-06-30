package dev.maliik.relaycore;

import org.springframework.boot.SpringApplication;

public class TestRelaycoreApplication {

	public static void main(String[] args) {
		SpringApplication.from(RelaycoreApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
