package com.meritocra.corootmcp.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meritocra.corootmcp.config.CorootProperties;
import com.meritocra.corootmcp.coroot.StubCorootClient;
import com.meritocra.corootmcp.support.FakeChatModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class SummarizeIncidentRootCauseToolTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private SummarizeIncidentRootCauseTool tool;

	@BeforeEach
	void setUp() {
		CorootProperties properties = new CorootProperties();
		properties.setApiUrl(URI.create("https://coroot.example.com"));
		properties.setDefaultProjectId("production");

		ChatClient chatClient = ChatClient.builder(new FakeChatModel()).build();
		tool = new SummarizeIncidentRootCauseTool(new StubCorootClient(), properties, chatClient, objectMapper);
	}

	@Test
	void givenTool_whenReadingDefinition_thenContainsExpectedNameAndSchema() {
		// when
		var definition = tool.definition();

		// then
		assertThat(definition.getName()).isEqualTo("summarize_incident_root_cause");
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
	void givenValidArguments_whenCallingTool_thenReturnsTextAndJsonItems() {
		// given
		ObjectNode args = objectMapper.createObjectNode();
		args.put("projectId", "production");
		args.put("incidentId", "inc-1");
		args.put("maxWords", 100);
		args.put("includeMetricsTable", true);

		// when
		ObjectNode result = tool.call(args);

		// then
		var content = result.path("content");
		assertThat(content.isArray()).isTrue();
		assertThat(content).hasSize(2);

		assertThat(content.get(0).path("type").asText()).isEqualTo("text");
		assertThat(content.get(0).path("text").asText()).isEqualTo("FAKE_SUMMARY");

		assertThat(content.get(1).path("type").asText()).isEqualTo("json");
		assertThat(content.get(1).path("json").path("incidentId").asText()).isEqualTo("inc-1");
	}
}
