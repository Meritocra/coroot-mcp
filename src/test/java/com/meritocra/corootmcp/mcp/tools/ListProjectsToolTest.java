package com.meritocra.corootmcp.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meritocra.corootmcp.coroot.StubCorootClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListProjectsToolTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private ListProjectsTool tool;

	@BeforeEach
	void setUp() {
		tool = new ListProjectsTool(new StubCorootClient(), objectMapper);
	}

	@Test
	void givenTool_whenReadingDefinition_thenContainsExpectedNameAndSchema() {
		// when
		var definition = tool.definition();

		// then
		assertThat(definition.getName()).isEqualTo("list_projects");
		assertThat(definition.getInputSchema().path("type").asText()).isEqualTo("object");
	}

	@Test
	void givenNoArguments_whenCallingTool_thenReturnsJsonArrayOfProjects() {
		// given
		ObjectNode args = objectMapper.createObjectNode();

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

}

