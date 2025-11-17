package com.meritocra.corootmcp.mcp.tools;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meritocra.corootmcp.config.CorootProperties;
import com.meritocra.corootmcp.coroot.CorootClient;
import com.meritocra.corootmcp.coroot.RiskOverviewEntry;
import com.meritocra.corootmcp.mcp.McpTool;
import com.meritocra.corootmcp.mcp.ToolDefinition;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GetRisksOverviewTool implements McpTool {

	private final CorootClient corootClient;

	private final CorootProperties properties;

	private final ObjectMapper objectMapper;

	public GetRisksOverviewTool(CorootClient corootClient, CorootProperties properties, ObjectMapper objectMapper) {
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

		return new ToolDefinition("get_risks_overview",
				"Returns a summary of risks detected in a Coroot project, including exposure and availability information.",
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

		List<RiskOverviewEntry> risks = corootClient.listRisksOverview(projectId);

		ObjectNode result = objectMapper.createObjectNode();
		ArrayNode content = result.putArray("content");

		ObjectNode jsonItem = content.addObject();
		jsonItem.put("type", "json");

		ArrayNode risksArray = objectMapper.createArrayNode();
		for (RiskOverviewEntry risk : risks) {
			ObjectNode node = objectMapper.createObjectNode();
			node.put("projectId", risk.getProjectId());
			node.put("service", risk.getService());
			node.put("cluster", risk.getCluster());
			node.put("category", risk.getCategory());
			node.put("severity", risk.getSeverity());
			node.put("type", risk.getType());
			node.putPOJO("exposure", risk.getExposure());
			node.put("availability", risk.getAvailability());
			risksArray.add(node);
		}

		jsonItem.set("json", risksArray);

		return result;
	}

}

