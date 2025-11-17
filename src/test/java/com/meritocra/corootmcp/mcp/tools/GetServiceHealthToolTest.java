package com.meritocra.corootmcp.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meritocra.corootmcp.config.CorootProperties;
import com.meritocra.corootmcp.coroot.StubCorootClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetServiceHealthToolTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private GetServiceHealthTool tool;

	@BeforeEach
	void setUp() {
		CorootProperties properties = new CorootProperties();
		properties.setApiUrl(URI.create("https://coroot.example.com"));
		properties.setDefaultProjectId("production");

		tool = new GetServiceHealthTool(new StubCorootClient(), properties, objectMapper);
	}

	@Test
	void givenTool_whenReadingDefinition_thenContainsExpectedNameAndSchema() {
		// when
		var definition = tool.definition();

		// then
		assertThat(definition.getName()).isEqualTo("get_service_health");
		assertThat(definition.getInputSchema().path("type").asText()).isEqualTo("object");
		assertThat(definition.getInputSchema().path("properties").has("service")).isTrue();
	}

	@Test
	void givenMissingService_whenCallingTool_thenFailsWithHelpfulError() {
		// given
		ObjectNode args = objectMapper.createObjectNode();
		args.put("projectId", "production");

		// when / then
		assertThatThrownBy(() -> tool.call(args))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("service is required");
	}

	@Test
	void givenValidArguments_whenCallingTool_thenReturnsJsonSnapshot() {
		// given
		ObjectNode args = objectMapper.createObjectNode();
		args.put("projectId", "production");
		args.put("service", "checkout-service");

		// when
		ObjectNode result = tool.call(args);

		// then
		var content = result.path("content");
		assertThat(content.isArray()).isTrue();
		assertThat(content).hasSize(1);

		var jsonItem = content.get(0);
		assertThat(jsonItem.path("type").asText()).isEqualTo("json");
		assertThat(jsonItem.path("json").path("projectId").asText()).isEqualTo("production");
		assertThat(jsonItem.path("json").path("service").asText()).isEqualTo("checkout-service");
		assertThat(jsonItem.path("json").path("indicators").isObject()).isTrue();
	}

}

