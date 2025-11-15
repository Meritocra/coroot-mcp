package com.meritocra.corootmcp.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

class McpToolRegistryTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static class EchoTool implements McpTool {

		@Override
		public ToolDefinition definition() {
			return new ToolDefinition("echo", "Echoes arguments", new ObjectMapper().createObjectNode());
		}

		@Override
		public ObjectNode call(ObjectNode arguments) {
			ObjectNode result = new ObjectMapper().createObjectNode();
			result.set("echo", arguments);
			return result;
		}
	}

	@Test
	void registersAndListsTools() {
		McpTool echo = new EchoTool();
		McpToolRegistry registry = new McpToolRegistry(List.of(echo));

		assertThat(registry.allTools()).containsExactly(echo);
		assertThat(registry.findTool("echo")).isSameAs(echo);
	}

	@Test
	void callsToolByName() {
		McpToolRegistry registry = new McpToolRegistry(List.of(new EchoTool()));
		ObjectNode args = objectMapper.createObjectNode();
		args.put("message", "hello");

		ObjectNode result = registry.call("echo", args);

		assertThat(result.path("echo").path("message").asText()).isEqualTo("hello");
	}

	@Test
	void throwsOnUnknownTool() {
		McpToolRegistry registry = new McpToolRegistry(List.of(new EchoTool()));

		assertThatThrownBy(() -> registry.call("missing", objectMapper.createObjectNode()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown tool");
	}
}

