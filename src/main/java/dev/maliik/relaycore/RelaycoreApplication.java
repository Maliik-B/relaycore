package dev.maliik.relaycore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Auth is stateless JWT (see common.security) — exclude Boot's default in-memory user/password autoconfig.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class RelaycoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(RelaycoreApplication.class, args);
	}

}
