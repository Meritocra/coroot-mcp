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

class GetIncidentDetailsToolTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private GetIncidentDetailsTool tool;

	@BeforeEach
	void setUp() {
		CorootProperties properties = new CorootProperties();
		properties.setApiUrl(URI.create("https://coroot.example.com"));
		properties.setDefaultProjectId("production");

		tool = new GetIncidentDetailsTool(new StubCorootClient(), properties, objectMapper);
	}

	@Test
	void givenTool_whenReadingDefinition_thenContainsExpectedNameAndSchema() {
		// when
		var definition = tool.definition();

		// then
		assertThat(definition.getName()).isEqualTo("get_incident_details");
		assertThat(definition.getInputSchema().path("type").asText()).isEqualTo("object");
		assertThat(definition.getInputSchema().path("properties").has("incidentId")).isTrue();
	}

	@Test
	void givenMissingIncidentId_whenCallingTool_thenFailsWithHelpfulError() {
		// given
		ObjectNode args = objectMapper.createObjectNode();
		args.put("projectId", "production");

		// when / then
		assertThatThrownBy(() -> tool.call(args))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("incidentId is required");
	}

	@Test
	void givenValidArguments_whenCallingTool_thenReturnsDetailedIncidentJson() {
		// given
		ObjectNode args = objectMapper.createObjectNode();
		args.put("projectId", "production");
		args.put("incidentId", "incident-1");

		// when
		ObjectNode result = tool.call(args);

		// then
		var content = result.path("content");
		assertThat(content.isArray()).isTrue();
		assertThat(content).hasSize(1);

		var jsonItem = content.get(0);
		assertThat(jsonItem.path("type").asText()).isEqualTo("json");

		var incidentJson = jsonItem.path("json");
		assertThat(incidentJson.path("incidentId").asText()).isEqualTo("incident-1");
		assertThat(incidentJson.path("title").asText()).isNotBlank();
		assertThat(incidentJson.path("service").asText()).isNotBlank();
		assertThat(incidentJson.path("affectedServices").isArray()).isTrue();
		assertThat(incidentJson.path("metricsSnapshot").isObject()).isTrue();
		assertThat(incidentJson.path("timeline").isArray()).isTrue();
	}

}

