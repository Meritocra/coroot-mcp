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
	void givenTool_whenReadingDefinition_thenContainsNameAndObjectSchema() {
		// when
		var definition = tool.definition();

		// then
		assertThat(definition.getName()).isEqualTo("list_recent_incidents");
		assertThat(definition.getInputSchema().path("type").asText())
				.isEqualTo("object");
	}

	@Test
	void givenNoProjectIdAndNoDefault_whenCallingTool_thenFailsWithHelpfulError() {
		// given
		CorootProperties emptyProps = new CorootProperties();
		ListRecentIncidentsTool toolWithoutDefault = new ListRecentIncidentsTool(
				new StubCorootClient(), emptyProps, objectMapper);

		ObjectNode args = objectMapper.createObjectNode();

		// when / then
		assertThatThrownBy(() -> toolWithoutDefault.call(args))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("projectId is required");
	}

	@Test
	void givenValidArguments_whenCallingTool_thenReturnsJsonArrayOfIncidents() {
		// given
		ObjectNode args = objectMapper.createObjectNode();
		args.put("projectId", "production");
		args.put("minimumSeverity", IncidentSeverity.WARNING.name());
		args.put("limit", 5);

		// when
		ObjectNode result = tool.call(args);

		// then
		var content = result.path("content");
		assertThat(content.isArray()).isTrue();
		assertThat(content).hasSize(1);

		var jsonItem = content.get(0);
		assertThat(jsonItem.path("type").asText()).isEqualTo("json");
		assertThat(jsonItem.path("json").isArray()).isTrue();
		assertThat(jsonItem.path("json")).isNotEmpty();
	}

	@Test
	void givenLimitBelowMinimum_whenCallingTool_thenLimitIsClampedToAtLeastOneIncident() {
		// given
		ObjectNode args = objectMapper.createObjectNode();
		args.put("projectId", "production");
		args.put("minimumSeverity", IncidentSeverity.INFO.name());
		args.put("limit", 0);

		// when
		ObjectNode result = tool.call(args);

		// then
		var content = result.path("content");
		assertThat(content.isArray()).isTrue();
		assertThat(content.get(0).path("json").isArray()).isTrue();
		// Stub client returns 2 incidents; clamping to 1 should still produce a non-empty array.
		assertThat(content.get(0).path("json").size()).isGreaterThanOrEqualTo(1);
	}

	@Test
	void givenLimitAboveMaximum_whenCallingTool_thenLimitIsClampedToMaximum() {
		// given
		ObjectNode args = objectMapper.createObjectNode();
		args.put("projectId", "production");
		args.put("minimumSeverity", IncidentSeverity.INFO.name());
		args.put("limit", 500);

		// when
		ObjectNode result = tool.call(args);

		// then
		var content = result.path("content");
		assertThat(content.isArray()).isTrue();
		assertThat(content.get(0).path("json").isArray()).isTrue();
		// Stub client returns 2 incidents; clamping to 50 should still include both.
		assertThat(content.get(0).path("json").size()).isEqualTo(2);
	}
}
