package com.thelook.ms_auth;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requer infraestrutura (PostgreSQL, Redis) e variável de ambiente PASSWORD_ENCODER. Executar apenas em ambiente integrado.")
class MsAuthApplicationTests {

	@Test
	void contextLoads() {
	}

}
