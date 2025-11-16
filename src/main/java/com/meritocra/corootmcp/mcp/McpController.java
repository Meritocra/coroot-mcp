package com.meritocra.corootmcp.mcp;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.util.StringUtils;

@RestController
public class McpController {

	private static final Logger logger = LoggerFactory.getLogger(McpController.class);

	private final McpToolRegistry toolRegistry;

	private final ObjectMapper objectMapper;

	private final String authToken;

	public McpController(McpToolRegistry toolRegistry, ObjectMapper objectMapper,
			@org.springframework.beans.factory.annotation.Value("${mcp.auth-token:}") String authToken) {
		this.toolRegistry = toolRegistry;
		this.objectMapper = objectMapper;
		this.authToken = authToken;
	}

	@PostMapping(path = "/mcp", consumes = "application/json", produces = "application/json")
	public ResponseEntity<ObjectNode> handle(@RequestBody ObjectNode request,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		if (StringUtils.hasText(this.authToken)) {
			String expected = "Bearer " + this.authToken;
			if (!expected.equals(authorizationHeader)) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			}
		}

		long start = System.nanoTime();
		String method = request.path("method").asText();
		ObjectNode response = objectMapper.createObjectNode();
		response.put("jsonrpc", "2.0");
		response.set("id", request.get("id"));

		String toolName = null;

		try {
			switch (method) {
				case "initialize" -> response.set("result", handleInitialize());
				case "tools/list" -> response.set("result", handleToolsList());
				case "tools/call" -> {
					toolName = request.path("params").path("name").asText(null);
					response.set("result", handleToolsCall(request.path("params")));
				}
				default -> response.set("error", error(-32601, "Method not found: " + method));
			}
		}
		catch (IllegalArgumentException ex) {
			response.set("error", error(-32602, ex.getMessage()));
		}
		catch (Exception ex) {
			response.set("error", error(-32603, "Internal error: " + ex.getMessage()));
		}

		long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
		logger.info("mcpRequest method={} tool={} durationMs={}", method, toolName, durationMs);

		return ResponseEntity.ok(response);
	}

	private ObjectNode handleInitialize() {
		ObjectNode result = objectMapper.createObjectNode();
		result.put("protocolVersion", "2024-11-05");

		ObjectNode serverInfo = result.putObject("serverInfo");
		serverInfo.put("name", "coroot-mcp");
		serverInfo.put("version", "0.1.0");
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
