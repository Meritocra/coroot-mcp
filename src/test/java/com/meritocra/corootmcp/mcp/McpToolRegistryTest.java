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
	void givenTool_whenRegistryCreated_thenToolIsRegisteredAndListed() {
		// given
		McpTool echo = new EchoTool();
		McpToolRegistry registry = new McpToolRegistry(List.of(echo));

		// then
		assertThat(registry.allTools()).containsExactly(echo);
		assertThat(registry.findTool("echo")).isSameAs(echo);
	}

	@Test
	void givenToolName_whenCallingRegistry_thenDelegatesToTool() {
		// given
		McpToolRegistry registry = new McpToolRegistry(List.of(new EchoTool()));
		ObjectNode args = objectMapper.createObjectNode();
		args.put("message", "hello");

		// when
		ObjectNode result = registry.call("echo", args);

		// then
		assertThat(result.path("echo").path("message").asText()).isEqualTo("hello");
	}

	@Test
	void givenUnknownToolName_whenCallingRegistry_thenThrowsHelpfulError() {
		// given
		McpToolRegistry registry = new McpToolRegistry(List.of(new EchoTool()));

		// when / then
		assertThatThrownBy(() -> registry.call("missing", objectMapper.createObjectNode()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown tool");
	}
}
