package com.meritocra.corootmcp.mcp;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface McpTool {

	ToolDefinition definition();

	ObjectNode call(ObjectNode arguments);
}

