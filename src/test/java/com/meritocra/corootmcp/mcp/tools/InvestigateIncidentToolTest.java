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

class InvestigateIncidentToolTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private InvestigateIncidentTool tool;

	@BeforeEach
	void setUp() {
		CorootProperties properties = new CorootProperties();
		properties.setApiUrl(URI.create("https://coroot.example.com"));
		properties.setDefaultProjectId("production");

		ChatClient chatClient = ChatClient.builder(new FakeChatModel()).build();

		tool = new InvestigateIncidentTool(new StubCorootClient(), properties, chatClient, objectMapper);
	}

	@Test
	void givenTool_whenReadingDefinition_thenContainsExpectedNameAndSchema() {
		// when
		var definition = tool.definition();

		// then
		assertThat(definition.getName()).isEqualTo("investigate_incident");
		assertThat(definition.getInputSchema().path("type").asText()).isEqualTo("object");
	}

	@Test
	void givenNoProjectIdAndNoDefault_whenCallingTool_thenFailsWithHelpfulError() {
		// given
		CorootProperties emptyProps = new CorootProperties();
		ChatClient chatClient = ChatClient.builder(new FakeChatModel()).build();

		InvestigateIncidentTool toolWithoutDefault = new InvestigateIncidentTool(
				new StubCorootClient(), emptyProps, chatClient, objectMapper);

		ObjectNode args = objectMapper.createObjectNode();
		args.put("incidentId", "incident-1");

		// when / then
		assertThatThrownBy(() -> toolWithoutDefault.call(args))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("projectId is required");
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
	void givenValidArguments_whenCallingTool_thenReturnsTextAndJson() {
		// given
		ObjectNode args = objectMapper.createObjectNode();
		args.put("projectId", "production");
		args.put("incidentId", "incident-1");
		args.put("maxWords", 400);
		args.put("audience", "sre");

		// when
		ObjectNode result = tool.call(args);

		// then
		var content = result.path("content");
		assertThat(content.isArray()).isTrue();
		assertThat(content).hasSize(2);

		var textItem = content.get(0);
		assertThat(textItem.path("type").asText()).isEqualTo("text");
		assertThat(textItem.path("text").asText()).isEqualTo("FAKE_SUMMARY");

		var jsonItem = content.get(1);
		assertThat(jsonItem.path("type").asText()).isEqualTo("json");
		var json = jsonItem.path("json");
		assertThat(json.path("incidentId").asText()).isEqualTo("incident-1");
		assertThat(json.path("projectId").asText()).isEqualTo("production");
	}

}
