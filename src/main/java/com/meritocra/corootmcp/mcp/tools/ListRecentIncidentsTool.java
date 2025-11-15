package com.meritocra.corootmcp.mcp.tools;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meritocra.corootmcp.config.CorootProperties;
import com.meritocra.corootmcp.coroot.CorootClient;
import com.meritocra.corootmcp.coroot.IncidentSeverity;
import com.meritocra.corootmcp.coroot.IncidentSummary;
import com.meritocra.corootmcp.mcp.McpTool;
import com.meritocra.corootmcp.mcp.ToolDefinition;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ListRecentIncidentsTool implements McpTool {

	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	private final CorootClient corootClient;

	private final CorootProperties properties;

	private final ObjectMapper objectMapper;

	public ListRecentIncidentsTool(CorootClient corootClient, CorootProperties properties, ObjectMapper objectMapper) {
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

		ObjectNode minimumSeverity = propertiesNode.putObject("minimumSeverity");
		minimumSeverity.put("type", "string");
		minimumSeverity.put("enum", objectMapper.createArrayNode()
				.add(IncidentSeverity.INFO.name())
				.add(IncidentSeverity.WARNING.name())
				.add(IncidentSeverity.CRITICAL.name()));
		minimumSeverity.put("description",
				"Minimum incident severity to include. Defaults to WARNING.");

		ObjectNode limit = propertiesNode.putObject("limit");
		limit.put("type", "integer");
		limit.put("minimum", 1);
		limit.put("maximum", 50);
		limit.put("description", "Maximum number of incidents to return. Defaults to 10.");

		schema.put("additionalProperties", false);

		return new ToolDefinition("list_recent_incidents",
				"Returns a compact list of recent Coroot incidents suitable for follow-up analysis or selection.",
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

		String severityText = arguments.path("minimumSeverity").asText(IncidentSeverity.WARNING.name());
		IncidentSeverity minimumSeverity = IncidentSeverity.valueOf(severityText);

		int limit = arguments.path("limit").asInt(10);

		List<IncidentSummary> incidents = corootClient.listRecentIncidents(projectId, minimumSeverity, limit);

		ObjectNode result = objectMapper.createObjectNode();
		ArrayNode content = result.putArray("content");

		ObjectNode jsonItem = content.addObject();
		jsonItem.put("type", "json");

		ArrayNode incidentsArray = objectMapper.createArrayNode();
		for (IncidentSummary incident : incidents) {
			ObjectNode node = objectMapper.createObjectNode();
			node.put("id", incident.getId());
			node.put("title", incident.getTitle());
			node.put("service", incident.getService());
			node.put("severity", incident.getSeverity().name());
			if (incident.getStartedAt() != null) {
				node.put("startedAt", ISO_FORMATTER.format(incident.getStartedAt().atOffset(ZoneOffset.UTC)));
			}
			if (incident.getEndedAt() != null) {
				node.put("endedAt", ISO_FORMATTER.format(incident.getEndedAt().atOffset(ZoneOffset.UTC)));
			}
			incidentsArray.add(node);
		}

		jsonItem.set("json", incidentsArray);

		return result;
	}
}

