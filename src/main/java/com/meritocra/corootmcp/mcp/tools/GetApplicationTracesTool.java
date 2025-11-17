package com.meritocra.corootmcp.mcp.tools;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meritocra.corootmcp.config.CorootProperties;
import com.meritocra.corootmcp.coroot.CorootClient;
import com.meritocra.corootmcp.mcp.McpTool;
import com.meritocra.corootmcp.mcp.ToolDefinition;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GetApplicationTracesTool implements McpTool {

	private static final int MAX_SPANS_CAP = 100;

	private final CorootClient corootClient;

	private final CorootProperties properties;

	private final ObjectMapper objectMapper;

	public GetApplicationTracesTool(CorootClient corootClient, CorootProperties properties, ObjectMapper objectMapper) {
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

		ObjectNode applicationId = propertiesNode.putObject("applicationId");
		applicationId.put("type", "string");
		applicationId.put("description",
				"Coroot application identifier like 'cluster:namespace:Kind:name'. Use ids from application health views.");

		ObjectNode windowMinutes = propertiesNode.putObject("windowMinutes");
		windowMinutes.put("type", "integer");
		windowMinutes.put("minimum", 1);
		windowMinutes.put("maximum", 1440);
		windowMinutes.put("description",
				"Time window, in minutes, to retrieve spans for. Defaults to 30 minutes.");

		ObjectNode maxSpans = propertiesNode.putObject("maxSpans");
		maxSpans.put("type", "integer");
		maxSpans.put("minimum", 1);
		maxSpans.put("maximum", MAX_SPANS_CAP);
		maxSpans.put("description",
				"Maximum number of spans to return, capped at " + MAX_SPANS_CAP + ".");

		ObjectNode required = schema.putArray("required").addObject();
		required.put("0", "applicationId");

		schema.put("additionalProperties", false);

		return new ToolDefinition("get_application_traces",
				"Returns recent tracing spans for a single Coroot application, summarised for AI-assisted analysis.",
				schema);
	}

	@Override
	@SuppressWarnings("unchecked")
	public ObjectNode call(ObjectNode arguments) {
		String projectId = arguments.path("projectId").asText();
		if (!StringUtils.hasText(projectId)) {
			projectId = properties.getDefaultProjectId();
		}
		if (!StringUtils.hasText(projectId)) {
			throw new IllegalArgumentException("projectId is required when coroot.default-project-id is not configured");
		}

		String applicationId = arguments.path("applicationId").asText(null);
		if (!StringUtils.hasText(applicationId)) {
			throw new IllegalArgumentException("applicationId is required");
		}

		int windowMinutes = arguments.path("windowMinutes").asInt(30);
		if (windowMinutes <= 0) {
			windowMinutes = 30;
		}
		if (windowMinutes > 24 * 60) {
			windowMinutes = 24 * 60;
		}

		int maxSpans = arguments.path("maxSpans").asInt(50);
		if (maxSpans <= 0) {
			maxSpans = 50;
		}
		if (maxSpans > MAX_SPANS_CAP) {
			maxSpans = MAX_SPANS_CAP;
		}

		Map<String, Object> view = corootClient.getApplicationTracing(projectId, applicationId, windowMinutes);

		List<Map<String, Object>> spans = (List<Map<String, Object>>) view.getOrDefault("spans", List.of());

		ObjectNode result = objectMapper.createObjectNode();
		ArrayNode content = result.putArray("content");

		ObjectNode jsonItem = content.addObject();
		jsonItem.put("type", "json");

		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("projectId", projectId);
		payload.put("applicationId", applicationId);
		payload.put("windowMinutes", windowMinutes);

		ArrayNode spansArray = payload.putArray("spans");
		int count = 0;
		for (Map<String, Object> span : spans) {
			if (count >= maxSpans) {
				break;
			}
			ObjectNode s = objectMapper.createObjectNode();
			s.put("service", stringOrEmpty(span.get("service")));
			s.put("traceId", stringOrEmpty(span.get("trace_id")));
			s.put("spanId", stringOrEmpty(span.get("id")));
			s.put("parentSpanId", stringOrEmpty(span.get("parent_id")));
			s.put("name", stringOrEmpty(span.get("name")));
			s.put("timestampMs", longOrZero(span.get("timestamp")));
			s.put("durationMs", doubleOrZero(span.get("duration")));
			s.put("status", stringOrEmpty(span.get("status")));
			s.put("client", stringOrEmpty(span.get("client")));
			spansArray.add(s);
			count++;
		}
		payload.put("spansReturned", count);
		payload.put("spansTotal", spans.size());

		jsonItem.set("json", payload);

		return result;
	}

	private String stringOrEmpty(Object value) {
		return value == null ? "" : value.toString();
	}

	private long longOrZero(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		try {
			return value != null ? Long.parseLong(value.toString()) : 0L;
		}
		catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private double doubleOrZero(Object value) {
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		try {
			return value != null ? Double.parseDouble(value.toString()) : 0.0;
		}
		catch (NumberFormatException ex) {
			return 0.0;
		}
	}

}

