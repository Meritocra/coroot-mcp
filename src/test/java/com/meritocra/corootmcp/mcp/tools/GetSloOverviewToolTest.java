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

class GetSloOverviewToolTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private GetSloOverviewTool tool;

	@BeforeEach
	void setUp() {
		CorootProperties properties = new CorootProperties();
		properties.setApiUrl(URI.create("https://coroot.example.com"));
		properties.setDefaultProjectId("production");

		tool = new GetSloOverviewTool(new StubCorootClient(), properties, objectMapper);
	}

	@Test
	void givenTool_whenReadingDefinition_thenContainsExpectedNameAndSchema() {
		// when
		var definition = tool.definition();

		// then
		assertThat(definition.getName()).isEqualTo("get_slo_overview");
		assertThat(definition.getInputSchema().path("type").asText()).isEqualTo("object");
	}

	@Test
	void givenNoProjectIdAndNoDefault_whenCallingTool_thenFailsWithHelpfulError() {
		// given
		CorootProperties emptyProps = new CorootProperties();
		GetSloOverviewTool toolWithoutDefault = new GetSloOverviewTool(
				new StubCorootClient(), emptyProps, objectMapper);

		ObjectNode args = objectMapper.createObjectNode();

		// when / then
		assertThatThrownBy(() -> toolWithoutDefault.call(args))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("projectId is required");
	}

	@Test
	void givenValidArguments_whenCallingTool_thenReturnsSloOverviewJson() {
		// given
		ObjectNode args = objectMapper.createObjectNode();
		args.put("projectId", "production");

		// when
		ObjectNode result = tool.call(args);

		// then
		var content = result.path("content");
		assertThat(content.isArray()).isTrue();
		assertThat(content).hasSize(1);

		var jsonItem = content.get(0);
		assertThat(jsonItem.path("type").asText()).isEqualTo("json");

		var overviewJson = jsonItem.path("json");
		assertThat(overviewJson.path("projectId").asText()).isEqualTo("production");
		assertThat(overviewJson.path("availability").isObject()).isTrue();
		assertThat(overviewJson.path("latency").isObject()).isTrue();
	}

}

