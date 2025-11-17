package com.meritocra.corootmcp.mcp.tools;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meritocra.corootmcp.coroot.CorootClient;
import com.meritocra.corootmcp.coroot.ProjectSummary;
import com.meritocra.corootmcp.mcp.McpTool;
import com.meritocra.corootmcp.mcp.ToolDefinition;

import org.springframework.stereotype.Component;

@Component
public class ListProjectsTool implements McpTool {

	private final CorootClient corootClient;

	private final ObjectMapper objectMapper;

	public ListProjectsTool(CorootClient corootClient, ObjectMapper objectMapper) {
		this.corootClient = corootClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public ToolDefinition definition() {
		ObjectNode schema = objectMapper.createObjectNode();
		schema.put("type", "object");
		schema.putObject("properties");
		schema.put("additionalProperties", false);

		return new ToolDefinition("list_projects",
				"Lists Coroot projects that the current Coroot API key can access.",
				schema);
	}

	@Override
	public ObjectNode call(ObjectNode arguments) {
		List<ProjectSummary> projects = corootClient.listProjects();

		ObjectNode result = objectMapper.createObjectNode();
		ArrayNode content = result.putArray("content");

		ObjectNode jsonItem = content.addObject();
		jsonItem.put("type", "json");

		ArrayNode projectsArray = objectMapper.createArrayNode();
		for (ProjectSummary project : projects) {
			ObjectNode node = objectMapper.createObjectNode();
			node.put("id", project.getId());
			node.put("name", project.getName());
			projectsArray.add(node);
		}

		jsonItem.set("json", projectsArray);

		return result;
	}

}

