package dev.maliik.relaycore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Context smoke test. Uses the same {@code RANDOM_PORT} web environment as the integration tests so it
 * shares their one cached context (and its single set of Testcontainers) rather than spinning up a
 * second container set — see the "unique context = own container set" gotcha.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RelaycoreApplicationTests {

	@Test
	void contextLoads() {
	}

}
