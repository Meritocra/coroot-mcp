package com.meritocra.corootmcp.mcp.tools;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meritocra.corootmcp.config.CorootProperties;
import com.meritocra.corootmcp.coroot.CorootClient;
import com.meritocra.corootmcp.coroot.NodeOverviewEntry;
import com.meritocra.corootmcp.mcp.McpTool;
import com.meritocra.corootmcp.mcp.ToolDefinition;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GetNodesOverviewTool implements McpTool {

	private final CorootClient corootClient;

	private final CorootProperties properties;

	private final ObjectMapper objectMapper;

	public GetNodesOverviewTool(CorootClient corootClient, CorootProperties properties, ObjectMapper objectMapper) {
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

		return new ToolDefinition("get_nodes_overview",
				"Returns an overview of node health for a Coroot project, aligned with the Nodes overview in Coroot.",
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

		List<NodeOverviewEntry> entries = corootClient.listNodesOverview(projectId);

		ObjectNode result = objectMapper.createObjectNode();
		ArrayNode content = result.putArray("content");

		ObjectNode jsonItem = content.addObject();
		jsonItem.put("type", "json");

		ArrayNode nodesArray = objectMapper.createArrayNode();
		for (NodeOverviewEntry entry : entries) {
			ObjectNode node = objectMapper.createObjectNode();
			node.put("projectId", entry.getProjectId());
			node.put("name", entry.getName());
			node.put("cluster", entry.getCluster());
			node.put("status", entry.getStatus());
			node.put("applications", entry.getApplications());
			node.put("instances", entry.getInstances());
			node.put("uptime", entry.getUptime());
			node.set("privateIps", objectMapper.valueToTree(entry.getPrivateIps()));
			node.set("publicIps", objectMapper.valueToTree(entry.getPublicIps()));
			if (entry.getCpuPercent() != null) {
				node.put("cpuPercent", entry.getCpuPercent());
			}
			if (entry.getMemoryPercent() != null) {
				node.put("memoryPercent", entry.getMemoryPercent());
			}
			if (entry.getNetworkPercent() != null) {
				node.put("networkPercent", entry.getNetworkPercent());
			}
			if (entry.getDiskPercent() != null) {
				node.put("diskPercent", entry.getDiskPercent());
			}
			nodesArray.add(node);
		}

		jsonItem.set("json", nodesArray);

		return result;
	}

}

