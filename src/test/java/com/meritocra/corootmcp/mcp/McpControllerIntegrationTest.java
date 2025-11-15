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
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestChatConfig.class)
class McpControllerIntegrationTest {

	@Autowired
	private ObjectMapper objectMapper;

	@LocalServerPort
	private int localServerPort;

	private final RestTemplate restTemplate = new RestTemplate();

	@Test
	void initializeAndListTools() throws Exception {
		JsonNode initResponse = postRpc("initialize", objectMapper.createObjectNode());

		assertThat(initResponse.path("result").path("protocolVersion").asText())
				.isNotBlank();

		JsonNode toolsResponse = postRpc("tools/list", objectMapper.createObjectNode());

		JsonNode tools = toolsResponse.path("result").path("tools");
		assertThat(tools.isArray()).isTrue();
		assertThat(tools).isNotEmpty();
	}

	@Test
	void toolsCallReturnsErrorForUnknownTool() throws Exception {
		var params = objectMapper.createObjectNode();
		params.put("name", "missing_tool");
		params.set("arguments", objectMapper.createObjectNode());

		JsonNode response = postRpc("tools/call", params);

		assertThat(response.path("error").path("code").asInt()).isEqualTo(-32602);
	}

	private JsonNode postRpc(String method, JsonNode params) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		var request = objectMapper.createObjectNode();
		request.put("jsonrpc", "2.0");
		request.put("id", "test");
		request.put("method", method);
		request.set("params", params);

		HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);

		String url = "http://localhost:" + localServerPort + "/mcp";
		ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

		return objectMapper.readTree(response.getBody());
	}
}
