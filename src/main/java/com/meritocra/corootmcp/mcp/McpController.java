package com.meritocra.corootmcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class McpController {

	private final McpToolRegistry toolRegistry;

	private final ObjectMapper objectMapper;

	public McpController(McpToolRegistry toolRegistry, ObjectMapper objectMapper) {
		this.toolRegistry = toolRegistry;
		this.objectMapper = objectMapper;
	}

	@PostMapping(path = "/mcp", consumes = "application/json", produces = "application/json")
	public ResponseEntity<ObjectNode> handle(@RequestBody ObjectNode request) {
		String method = request.path("method").asText();
		ObjectNode response = objectMapper.createObjectNode();
		response.put("jsonrpc", "2.0");
		response.set("id", request.get("id"));

		try {
			switch (method) {
				case "initialize" -> response.set("result", handleInitialize());
				case "tools/list" -> response.set("result", handleToolsList());
				case "tools/call" -> response.set("result", handleToolsCall(request.path("params")));
				default -> response.set("error", error(-32601, "Method not found: " + method));
			}
		}
		catch (IllegalArgumentException ex) {
			response.set("error", error(-32602, ex.getMessage()));
		}
		catch (Exception ex) {
			response.set("error", error(-32603, "Internal error: " + ex.getMessage()));
		}

		return ResponseEntity.ok(response);
	}

	private ObjectNode handleInitialize() {
		ObjectNode result = objectMapper.createObjectNode();
		result.put("protocolVersion", "2024-11-05");

		ObjectNode serverInfo = result.putObject("serverInfo");
		serverInfo.put("name", "coroot-mcp");
		serverInfo.put("version", "0.0.1");
		serverInfo.put("description",
				"MCP server that exposes Coroot root-cause analysis context as tools for LLM assistants.");

		ObjectNode capabilities = result.putObject("capabilities");
		ObjectNode toolsNode = capabilities.putObject("tools");
		toolsNode.put("listChanged", true);

		return result;
	}

	private ObjectNode handleToolsList() {
		ObjectNode result = objectMapper.createObjectNode();
		ArrayNode toolsArray = result.putArray("tools");

		for (McpTool tool : toolRegistry.allTools()) {
			ToolDefinition definition = tool.definition();
			ObjectNode node = objectMapper.createObjectNode();
			node.put("name", definition.getName());
			node.put("description", definition.getDescription());
			node.set("inputSchema", definition.getInputSchema());
			toolsArray.add(node);
		}

		return result;
	}

	private ObjectNode handleToolsCall(JsonNode params) {
		if (params == null || !params.isObject()) {
			throw new IllegalArgumentException("params must be an object");
		}

		ObjectNode paramsObject = (ObjectNode) params;

		String name = paramsObject.path("name").asText(null);
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Tool name is required in params.name");
		}
		ObjectNode arguments = paramsObject.with("arguments");
		return toolRegistry.call(name, arguments);
	}

	private ObjectNode error(int code, String message) {
		ObjectNode error = objectMapper.createObjectNode();
		error.put("code", code);
		error.put("message", message);
		return error;
	}
}
