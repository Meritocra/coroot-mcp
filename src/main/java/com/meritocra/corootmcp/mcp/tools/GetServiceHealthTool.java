package com.meritocra.corootmcp.mcp.tools;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meritocra.corootmcp.config.CorootProperties;
import com.meritocra.corootmcp.coroot.CorootClient;
import com.meritocra.corootmcp.coroot.ServiceHealthSnapshot;
import com.meritocra.corootmcp.mcp.McpTool;
import com.meritocra.corootmcp.mcp.ToolDefinition;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GetServiceHealthTool implements McpTool {

	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	private final CorootClient corootClient;

	private final CorootProperties properties;

	private final ObjectMapper objectMapper;

	public GetServiceHealthTool(CorootClient corootClient, CorootProperties properties, ObjectMapper objectMapper) {
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

		ObjectNode service = propertiesNode.putObject("service");
		service.put("type", "string");
		service.put("description",
				"Coroot service or application identifier, as shown in the Coroot UI.");

		ArrayNode required = schema.putArray("required");
		required.add("service");

		schema.put("additionalProperties", false);

		return new ToolDefinition("get_service_health",
				"Returns a compact health snapshot for a Coroot service, aligned with the Application Health Summary view.",
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

		String service = arguments.path("service").asText(null);
		if (!StringUtils.hasText(service)) {
			throw new IllegalArgumentException("service is required");
		}

		ServiceHealthSnapshot snapshot = corootClient.getServiceHealth(projectId, service);

		ObjectNode result = objectMapper.createObjectNode();
		ArrayNode content = result.putArray("content");

		ObjectNode jsonItem = content.addObject();
		jsonItem.put("type", "json");

		ObjectNode json = objectMapper.createObjectNode();
		json.put("projectId", snapshot.getProjectId());
		json.put("service", snapshot.getService());
		if (snapshot.getObservedAt() != null) {
			json.put("observedAt", ISO_FORMATTER.format(snapshot.getObservedAt().atOffset(ZoneOffset.UTC)));
		}
		json.putPOJO("indicators", snapshot.getIndicators());

		jsonItem.set("json", json);

		return result;
	}
}

