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

class GetApplicationTracesToolTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private GetApplicationTracesTool tool;

	@BeforeEach
	void setUp() {
		CorootProperties properties = new CorootProperties();
		properties.setApiUrl(URI.create("https://coroot.example.com"));
		properties.setDefaultProjectId("production");

		tool = new GetApplicationTracesTool(new StubCorootClient(), properties, objectMapper);
	}

	@Test
	void givenTool_whenReadingDefinition_thenContainsExpectedNameAndSchema() {
		// when
		var definition = tool.definition();

		// then
		assertThat(definition.getName()).isEqualTo("get_application_traces");
		assertThat(definition.getInputSchema().path("type").asText()).isEqualTo("object");
	}

	@Test
	void givenNoProjectIdAndNoDefault_whenCallingTool_thenFailsWithHelpfulError() {
		// given
		CorootProperties emptyProps = new CorootProperties();
		GetApplicationTracesTool toolWithoutDefault = new GetApplicationTracesTool(
				new StubCorootClient(), emptyProps, objectMapper);

		ObjectNode args = objectMapper.createObjectNode();
		args.put("applicationId", "cluster:ns:Deployment:checkout-service");

		// when / then
		assertThatThrownBy(() -> toolWithoutDefault.call(args))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("projectId is required");
	}

	@Test
	void givenMissingApplicationId_whenCallingTool_thenFailsWithHelpfulError() {
		// given
		ObjectNode args = objectMapper.createObjectNode();
		args.put("projectId", "production");

		// when / then
		assertThatThrownBy(() -> tool.call(args))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("applicationId is required");
	}

	@Test
	void givenValidArguments_whenCallingTool_thenReturnsTracesJson() {
		// given
		ObjectNode args = objectMapper.createObjectNode();
		args.put("projectId", "production");
		args.put("applicationId", "cluster:ns:Deployment:checkout-service");
		args.put("windowMinutes", 30);
		args.put("maxSpans", 10);

		// when
		ObjectNode result = tool.call(args);

		// then
		var content = result.path("content");
		assertThat(content.isArray()).isTrue();
		assertThat(content).hasSize(1);

		var jsonItem = content.get(0);
		assertThat(jsonItem.path("type").asText()).isEqualTo("json");

		var json = jsonItem.path("json");
		assertThat(json.path("projectId").asText()).isEqualTo("production");
		assertThat(json.path("applicationId").asText()).isEqualTo("cluster:ns:Deployment:checkout-service");
		assertThat(json.path("spans").isArray()).isTrue();
	}

}

