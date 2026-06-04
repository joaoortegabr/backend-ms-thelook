package com.thelook.api_gateway;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requer infraestrutura (Redis, Eureka) e variáveis de ambiente. Executar apenas em ambiente integrado.")
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
