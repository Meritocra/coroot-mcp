package com.meritocra.corootmcp.mcp;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "mcp.auth-token=test-secret-token" })
@Import(TestChatConfig.class)
class McpControllerAuthIntegrationTest {

	@Autowired
	private ObjectMapper objectMapper;

	@LocalServerPort
	private int localServerPort;

	private final RestTemplate restTemplate;

	McpControllerAuthIntegrationTest() {
		this.restTemplate = new RestTemplate();
		this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				// Allow tests to inspect non-2xx responses without throwing exceptions.
				return false;
			}
		});
	}

	@Test
	void givenAuthTokenConfigured_whenNoAuthorizationHeader_thenRequestIsRejected() throws Exception {
		// given
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		var request = objectMapper.createObjectNode();
		request.put("jsonrpc", "2.0");
		request.put("id", "test");
		request.put("method", "initialize");
		request.set("params", objectMapper.createObjectNode());

		HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);

		// when
		String url = "http://localhost:" + localServerPort + "/mcp";
		ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

		// then
		assertThat(response.getStatusCode().value()).isEqualTo(401);
	}

	@Test
	void givenAuthTokenConfigured_whenCorrectAuthorizationHeaderProvided_thenRequestSucceeds() throws Exception {
		// given
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, "Bearer test-secret-token");

		var request = objectMapper.createObjectNode();
		request.put("jsonrpc", "2.0");
		request.put("id", "test");
		request.put("method", "initialize");
		request.set("params", objectMapper.createObjectNode());

		HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);

		// when
		String url = "http://localhost:" + localServerPort + "/mcp";
		ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

		// then
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

		JsonNode body = objectMapper.readTree(response.getBody());
		assertThat(body.path("result").path("protocolVersion").asText()).isNotBlank();
	}

}
