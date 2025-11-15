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

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SummarizeIncidentRootCauseTool implements McpTool {

	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	private final CorootClient corootClient;

	private final CorootProperties properties;

	private final ChatClient chatClient;

	private final ObjectMapper objectMapper;

	public SummarizeIncidentRootCauseTool(CorootClient corootClient, CorootProperties properties,
			ChatClient chatClient, ObjectMapper objectMapper) {
		this.corootClient = corootClient;
		this.properties = properties;
		this.chatClient = chatClient;
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

		ObjectNode maxWords = propertiesNode.putObject("maxWords");
		maxWords.put("type", "integer");
		maxWords.put("minimum", 50);
		maxWords.put("maximum", 800);
		maxWords.put("description", "Upper bound on the length of the natural-language summary.");

		ObjectNode includeMetricsTable = propertiesNode.putObject("includeMetricsTable");
		includeMetricsTable.put("type", "boolean");
		includeMetricsTable.put("description",
				"Whether to include a compact JSON structure with key metrics that the model can render as a table.");

		ArrayNode required = schema.putArray("required");
		required.add("incidentId");

		schema.put("additionalProperties", false);

		return new ToolDefinition("summarize_incident_root_cause",
				"Summarizes a Coroot incident and explains the most likely root cause, blast radius, and next steps.",
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

		int maxWords = arguments.path("maxWords").asInt(300);
		boolean includeMetricsTable = arguments.path("includeMetricsTable").asBoolean(true);

		IncidentContext context = corootClient.getIncidentContext(projectId, incidentId);
		IncidentSummary summary = context.getSummary();

		String prompt = buildPrompt(context, maxWords);

		String naturalLanguageSummary = chatClient.prompt()
				.user(prompt)
				.call()
				.content();

		ObjectNode result = objectMapper.createObjectNode();
		ArrayNode content = result.putArray("content");

		ObjectNode textItem = content.addObject();
		textItem.put("type", "text");
		textItem.put("text", naturalLanguageSummary);

		if (includeMetricsTable) {
			ObjectNode jsonItem = content.addObject();
			jsonItem.put("type", "json");

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
			incidentJson.putPOJO("affectedServices", context.getAffectedServices());
			incidentJson.putPOJO("metricsSnapshot", context.getMetricsSnapshot());
			incidentJson.putPOJO("timeline", context.getTimeline());

			if (context.getLastUpdatedAt() != null) {
				incidentJson.put("lastUpdatedAt",
						ISO_FORMATTER.format(context.getLastUpdatedAt().atOffset(ZoneOffset.UTC)));
			}

			jsonItem.set("json", incidentJson);
		}

		return result;
	}

	private String buildPrompt(IncidentContext context, int maxWords) {
		IncidentSummary summary = context.getSummary();
		StringBuilder builder = new StringBuilder();

		builder.append(
				"You are a senior SRE using Coroot's service graph, checks, metrics, logs, traces and profiles to explain incidents to engineers.\n");
		builder.append("Summarize the incident for a human in at most ").append(maxWords)
				.append(" words.\n\n");
		builder.append("Focus on:\n");
		builder.append("- What actually broke and why.\n");
		builder.append("- The most likely root cause.\n");
		builder.append("- Blast radius in terms of services and user impact.\n");
		builder.append("- Concrete remediation steps and follow-up actions.\n\n");

		builder.append("Incident summary:\n");
		builder.append("ID: ").append(summary.getId()).append("\n");
		builder.append("Title: ").append(summary.getTitle()).append("\n");
		builder.append("Service: ").append(summary.getService()).append("\n");
		builder.append("Severity: ").append(summary.getSeverity().name()).append("\n\n");

		builder.append("Suspected root cause from Coroot heuristics:\n");
		builder.append(context.getSuspectedRootCause()).append("\n\n");

		builder.append("Affected services:\n");
		for (String service : context.getAffectedServices()) {
			builder.append("- ").append(service).append("\n");
		}
		builder.append("\n");

		builder.append("Key metrics snapshot (JSON):\n");
		builder.append(context.getMetricsSnapshot()).append("\n\n");

		builder.append("Timeline of important events:\n");
		for (String event : context.getTimeline()) {
			builder.append("- ").append(event).append("\n");
		}

		return builder.toString();
	}
}

