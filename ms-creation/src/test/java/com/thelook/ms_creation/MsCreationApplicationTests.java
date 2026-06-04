package com.thelook.ms_creation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requer infraestrutura (PostgreSQL, RabbitMQ). Executar apenas em ambiente integrado.")
class MsCreationApplicationTests {

	@Test
	void contextLoads() {
	}

}
