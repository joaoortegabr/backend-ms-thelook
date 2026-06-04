package com.thelook.ms_social;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requer infraestrutura (PostgreSQL, Neo4j, Redis). Executar apenas em ambiente integrado.")
class MsSocialApplicationTests {

	@Test
	void contextLoads() {
	}

}
