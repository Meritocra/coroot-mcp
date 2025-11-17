package com.meritocra.corootmcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meritocra.corootmcp.support.TestChatConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestChatConfig.class)
class HealthEndpointIntegrationTest {

	@LocalServerPort
	private int localServerPort;

	@Autowired
	private ObjectMapper objectMapper;

	private final RestTemplate restTemplate = new RestTemplate();

	@Test
	void givenRunningServer_whenCallingHealthEndpoint_thenStatusIsUp() throws Exception {
		// given
		String url = "http://localhost:" + localServerPort + "/actuator/health";
		// when
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

		// then
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

		JsonNode body = objectMapper.readTree(response.getBody());
		assertThat(body.path("status").asText()).isEqualTo("UP");
	}

}
