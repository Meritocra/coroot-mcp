package com.meritocra.corootmcp.mcp;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.stereotype.Component;

@Component
public class McpToolRegistry {

	private final Map<String, McpTool> toolsByName;

	public McpToolRegistry(Collection<McpTool> tools) {
		this.toolsByName = tools.stream()
				.collect(Collectors.toUnmodifiableMap(tool -> tool.definition().getName(), Function.identity()));
	}

	public Collection<McpTool> allTools() {
		return toolsByName.values();
	}

	public McpTool findTool(String name) {
		return toolsByName.get(name);
	}

	public ObjectNode call(String name, ObjectNode arguments) {
		McpTool tool = toolsByName.get(name);
		if (tool == null) {
			throw new IllegalArgumentException("Unknown tool: " + name);
		}
		return tool.call(arguments);
	}
}

