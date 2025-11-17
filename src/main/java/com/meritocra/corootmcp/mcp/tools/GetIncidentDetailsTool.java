package com.meritocra.corootmcp.mcp.tools;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meritocra.corootmcp.config.CorootProperties;
import com.meritocra.corootmcp.coroot.CorootClient;
import com.meritocra.corootmcp.coroot.IncidentContext;
import com.meritocra.corootmcp.coroot.IncidentSummary;
import com.meritocra.corootmcp.mcp.McpTool;
import com.meritocra.corootmcp.mcp.ToolDefinition;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GetIncidentDetailsTool implements McpTool {

	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	private final CorootClient corootClient;

	private final CorootProperties properties;

	private final ObjectMapper objectMapper;

	public GetIncidentDetailsTool(CorootClient corootClient, CorootProperties properties, ObjectMapper objectMapper) {
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

		ObjectNode incidentId = propertiesNode.putObject("incidentId");
		incidentId.put("type", "string");
		incidentId.put("description",
				"Identifier of the incident as seen in Coroot. Use this when a human references a specific incident.");

		ArrayNode required = schema.putArray("required");
		required.add("incidentId");

		schema.put("additionalProperties", false);

		return new ToolDefinition("get_incident_details",
				"Returns detailed Coroot incident context, including suspected root cause, affected services, metrics, and timeline.",
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

		String incidentId = arguments.path("incidentId").asText(null);
		if (!StringUtils.hasText(incidentId)) {
			throw new IllegalArgumentException("incidentId is required");
		}

		IncidentContext context = corootClient.getIncidentContext(projectId, incidentId);

		ObjectNode result = objectMapper.createObjectNode();
		ArrayNode content = result.putArray("content");

		ObjectNode jsonItem = content.addObject();
		jsonItem.put("type", "json");

		ObjectNode incidentJson = toJson(context);
		jsonItem.set("json", incidentJson);

		return result;
	}

	private ObjectNode toJson(IncidentContext context) {
		IncidentSummary summary = context.getSummary();

		ObjectNode incidentJson = objectMapper.createObjectNode();
		incidentJson.put("incidentId", summary.getId());
		incidentJson.put("title", summary.getTitle());
		incidentJson.put("severity", summary.getSeverity().name());
		incidentJson.put("service", summary.getService());

		if (summary.getStartedAt() != null) {
			incidentJson.put("startedAt", ISO_FORMATTER.format(summary.getStartedAt().atOffset(ZoneOffset.UTC)));
		}
		if (summary.getEndedAt() != null) {
			incidentJson.put("endedAt", ISO_FORMATTER.format(summary.getEndedAt().atOffset(ZoneOffset.UTC)));
		}

		incidentJson.put("suspectedRootCause", context.getSuspectedRootCause());
		incidentJson.set("affectedServices", objectMapper.valueToTree(context.getAffectedServices()));
		incidentJson.set("metricsSnapshot", objectMapper.valueToTree(context.getMetricsSnapshot()));
		incidentJson.set("timeline", objectMapper.valueToTree(context.getTimeline()));

		if (context.getLastUpdatedAt() != null) {
			incidentJson.put("lastUpdatedAt",
					ISO_FORMATTER.format(context.getLastUpdatedAt().atOffset(ZoneOffset.UTC)));
		}

		return incidentJson;
	}

}
