package com.meritocra.corootmcp.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meritocra.corootmcp.config.CorootProperties;
import com.meritocra.corootmcp.coroot.IncidentSeverity;
import com.meritocra.corootmcp.coroot.StubCorootClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListRecentIncidentsToolTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private ListRecentIncidentsTool tool;

	@BeforeEach
	void setUp() {
		CorootProperties properties = new CorootProperties();
		properties.setApiUrl(URI.create("https://coroot.example.com"));
		properties.setDefaultProjectId("production");

		tool = new ListRecentIncidentsTool(new StubCorootClient(), properties, objectMapper);
	}

	@Test
	void definitionContainsNameAndSchema() {
		assertThat(tool.definition().getName()).isEqualTo("list_recent_incidents");
		assertThat(tool.definition().getInputSchema().path("type").asText())
				.isEqualTo("object");
	}

	@Test
	void failsWhenProjectIdMissingAndNoDefaultConfigured() {
		CorootProperties emptyProps = new CorootProperties();
		ListRecentIncidentsTool toolWithoutDefault = new ListRecentIncidentsTool(
				new StubCorootClient(), emptyProps, objectMapper);

		ObjectNode args = objectMapper.createObjectNode();

		assertThatThrownBy(() -> toolWithoutDefault.call(args))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("projectId is required");
	}

	@Test
	void returnsJsonArrayOfIncidents() {
		ObjectNode args = objectMapper.createObjectNode();
		args.put("projectId", "production");
		args.put("minimumSeverity", IncidentSeverity.WARNING.name());
		args.put("limit", 5);

		ObjectNode result = tool.call(args);

		var content = result.path("content");
		assertThat(content.isArray()).isTrue();
		assertThat(content).hasSize(1);

		var jsonItem = content.get(0);
		assertThat(jsonItem.path("type").asText()).isEqualTo("json");
		assertThat(jsonItem.path("json").isArray()).isTrue();
		assertThat(jsonItem.path("json")).isNotEmpty();
	}
}

