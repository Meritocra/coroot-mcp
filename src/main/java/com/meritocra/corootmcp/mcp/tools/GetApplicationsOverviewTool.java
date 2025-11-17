package com.meritocra.corootmcp.mcp.tools;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meritocra.corootmcp.config.CorootProperties;
import com.meritocra.corootmcp.coroot.ApplicationOverviewEntry;
import com.meritocra.corootmcp.coroot.CorootClient;
import com.meritocra.corootmcp.mcp.McpTool;
import com.meritocra.corootmcp.mcp.ToolDefinition;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GetApplicationsOverviewTool implements McpTool {

	private final CorootClient corootClient;

	private final CorootProperties properties;

	private final ObjectMapper objectMapper;

	public GetApplicationsOverviewTool(CorootClient corootClient, CorootProperties properties,
			ObjectMapper objectMapper) {
		this.corootClient = corootClient;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Override
	public ToolDefinition definition() {
		ObjectNode schema = objectMapper.createObjectNode();
		schema.put("type", "object");

		ObjectNode propertiesNode = schema.putObject("properties");

		ObjectNode projectId = propertiesNode.putObject("projectId");
		projectId.put("type", "string");
		projectId.put("description",
				"Coroot project identifier. Defaults to coroot.default-project-id when omitted.");

		schema.put("additionalProperties", false);

		return new ToolDefinition("get_applications_overview",
				"Returns an overview of application health for a Coroot project, aligned with the Application Health Summary view.",
				schema);
	}

	@Override
	public ObjectNode call(ObjectNode arguments) {
		String projectId = arguments.path("projectId").asText();
		if (!StringUtils.hasText(projectId)) {
			projectId = properties.getDefaultProjectId();
		}
		if (!StringUtils.hasText(projectId)) {
			throw new IllegalArgumentException("projectId is required when coroot.default-project-id is not configured");
		}

		List<ApplicationOverviewEntry> entries = corootClient.listApplicationsOverview(projectId);

		ObjectNode result = objectMapper.createObjectNode();
		ArrayNode content = result.putArray("content");

		ObjectNode jsonItem = content.addObject();
		jsonItem.put("type", "json");

		ArrayNode appsArray = objectMapper.createArrayNode();
		for (ApplicationOverviewEntry entry : entries) {
			ObjectNode node = objectMapper.createObjectNode();
			node.put("projectId", entry.getProjectId());
			node.put("service", entry.getService());
			node.put("cluster", entry.getCluster());
			node.put("category", entry.getCategory());
			node.put("status", entry.getStatus());
			node.putPOJO("indicators", entry.getIndicators());
			appsArray.add(node);
		}

		jsonItem.set("json", appsArray);

		return result;
	}

}

