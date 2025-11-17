package com.meritocra.corootmcp.mcp.tools;

import java.time.ZoneOffset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meritocra.corootmcp.config.CorootProperties;
import com.meritocra.corootmcp.coroot.CorootClient;
import com.meritocra.corootmcp.coroot.IncidentContext;
import com.meritocra.corootmcp.coroot.IncidentSummary;
import com.meritocra.corootmcp.coroot.ServiceHealthSnapshot;
import com.meritocra.corootmcp.mcp.McpTool;
import com.meritocra.corootmcp.mcp.ToolDefinition;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InvestigateIncidentTool implements McpTool {

	private static final int MIN_WORDS = 100;

	private static final int MAX_WORDS = 1000;

	private final CorootClient corootClient;

	private final CorootProperties properties;

	private final ChatClient chatClient;

	private final ObjectMapper objectMapper;

	public InvestigateIncidentTool(CorootClient corootClient, CorootProperties properties,
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
				"Identifier of the incident as seen in Coroot. Use ids from the incidents list.");

		ObjectNode maxWords = propertiesNode.putObject("maxWords");
		maxWords.put("type", "integer");
		maxWords.put("minimum", MIN_WORDS);
		maxWords.put("maximum", MAX_WORDS);
		maxWords.put("description",
				"Upper bound on the length of the natural-language RCA summary.");

		ObjectNode audience = propertiesNode.putObject("audience");
		audience.put("type", "string");
		audience.put("description",
				"Target audience for the summary, for example 'sre' or 'exec'. Defaults to 'sre'.");

		ObjectNode includeTraces = propertiesNode.putObject("includeTraces");
		includeTraces.put("type", "boolean");
		includeTraces.put("description", "Whether to include tracing overviews in the analysis.");

		ObjectNode includeLogs = propertiesNode.putObject("includeLogs");
		includeLogs.put("type", "boolean");
		includeLogs.put("description", "Whether to include log overviews in the analysis.");

		ObjectNode includeCosts = propertiesNode.putObject("includeCosts");
		includeCosts.put("type", "boolean");
		includeCosts.put("description", "Whether to include costs overview in the analysis.");

		ObjectNode includeRisks = propertiesNode.putObject("includeRisks");
		includeRisks.put("type", "boolean");
		includeRisks.put("description", "Whether to include risk overview in the analysis.");

		ArrayNode required = schema.putArray("required");
		required.add("incidentId");

		schema.put("additionalProperties", false);

		return new ToolDefinition("investigate_incident",
				"Performs a structured incident investigation using Coroot data and returns an RCA summary "
						+ "plus a machine-readable JSON payload.",
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

		int maxWords = arguments.path("maxWords").asInt(600);
		if (maxWords < MIN_WORDS) {
			maxWords = MIN_WORDS;
		}
		if (maxWords > MAX_WORDS) {
			maxWords = MAX_WORDS;
		}

		String audience = arguments.path("audience").asText("sre").toLowerCase();
		boolean includeTraces = arguments.path("includeTraces").asBoolean(true);
		boolean includeLogs = arguments.path("includeLogs").asBoolean(true);
		boolean includeCosts = arguments.path("includeCosts").asBoolean(true);
		boolean includeRisks = arguments.path("includeRisks").asBoolean(true);

		IncidentContext context = corootClient.getIncidentContext(projectId, incidentId);

		// Build evidence bundle (compact JSON) to feed to the model and to expose to
		// downstream tooling.
		ObjectNode evidence = buildEvidence(projectId, context, includeTraces, includeLogs, includeCosts, includeRisks);

		String prompt = InvestigateIncidentPrompts.buildRcaPrompt(objectMapper, context, evidence, maxWords, audience);

		String naturalLanguageSummary = chatClient.prompt()
				.user(prompt)
				.call()
				.content();

		ObjectNode structured = buildStructuredSummary(projectId, context, evidence);

		ObjectNode result = objectMapper.createObjectNode();
		ArrayNode content = result.putArray("content");

		ObjectNode textItem = content.addObject();
		textItem.put("type", "text");
		textItem.put("text", naturalLanguageSummary);

		ObjectNode jsonItem = content.addObject();
		jsonItem.put("type", "json");
		jsonItem.set("json", structured);

		return result;
	}

	private ObjectNode buildEvidence(String projectId, IncidentContext context, boolean includeTraces,
			boolean includeLogs, boolean includeCosts, boolean includeRisks) {

		ObjectNode evidence = objectMapper.createObjectNode();

		// Incident details
		evidence.set("incident", toIncidentJson(context));

		// SLO overview at project level
		evidence.set("sloOverview", objectMapper.valueToTree(corootClient.getSloOverview(projectId)));

		IncidentSummary summary = context.getSummary();
		String primaryService = summary != null ? summary.getService() : null;

		// Service health (if we know the primary service)
		if (StringUtils.hasText(primaryService)) {
			ServiceHealthSnapshot healthSnapshot = corootClient.getServiceHealth(projectId, primaryService);
			evidence.set("serviceHealth", toServiceHealthJson(healthSnapshot));
		}

		if (includeCosts) {
			evidence.set("costsOverview", objectMapper.valueToTree(corootClient.getCostsOverview(projectId)));
		}
		if (includeTraces) {
			evidence.set("tracesOverview", objectMapper.valueToTree(corootClient.getTracesOverview(projectId, null)));
		}
		if (includeLogs) {
			evidence.set("logsOverview", objectMapper.valueToTree(corootClient.getLogsOverview(projectId, null)));
		}
		if (includeRisks) {
			evidence.set("risksOverview", objectMapper.valueToTree(corootClient.listRisksOverview(projectId)));
		}

		evidence.set("applicationsOverview", objectMapper.valueToTree(corootClient.listApplicationsOverview(projectId)));

		return evidence;
	}

	private ObjectNode buildStructuredSummary(String projectId, IncidentContext context, ObjectNode evidence) {
		ObjectNode root = objectMapper.createObjectNode();

		IncidentSummary summary = context.getSummary();
		String primaryService = summary != null ? summary.getService() : null;

		if (!StringUtils.hasText(primaryService) && !context.getAffectedServices().isEmpty()) {
			primaryService = context.getAffectedServices().get(0);
		}

		root.put("projectId", projectId);
		root.put("incidentId", summary != null ? summary.getId() : "");
		root.put("primaryService", primaryService != null ? primaryService : "");
		root.put("probableRootCause", context.getSuspectedRootCause());

		ArrayNode evidenceList = root.putArray("supportingEvidence");
		if (summary != null && summary.getStartedAt() != null && summary.getEndedAt() != null) {
			evidenceList.add("Incident duration: from " + summary.getStartedAt().atOffset(ZoneOffset.UTC)
					+ " to " + summary.getEndedAt().atOffset(ZoneOffset.UTC));
		}
		if (!context.getMetricsSnapshot().isEmpty()) {
			evidenceList.add("Metrics snapshot keys: " + context.getMetricsSnapshot().keySet());
		}

		// Embed SLO and cost data as-is so downstream tools can reason about them.
		root.set("sloOverview", evidence.get("sloOverview"));
		root.set("costsOverview", evidence.get("costsOverview"));

		return root;
	}

	private ObjectNode toIncidentJson(IncidentContext context) {
		IncidentSummary summary = context.getSummary();

		ObjectNode incidentJson = objectMapper.createObjectNode();
		if (summary != null) {
			incidentJson.put("incidentId", summary.getId());
			incidentJson.put("title", summary.getTitle());
			incidentJson.put("severity", summary.getSeverity().name());
			incidentJson.put("service", summary.getService());

			if (summary.getStartedAt() != null) {
				incidentJson.put("startedAt", summary.getStartedAt().atOffset(ZoneOffset.UTC).toString());
			}
			if (summary.getEndedAt() != null) {
				incidentJson.put("endedAt", summary.getEndedAt().atOffset(ZoneOffset.UTC).toString());
			}
		}

		incidentJson.put("suspectedRootCause", context.getSuspectedRootCause());
		incidentJson.set("affectedServices", objectMapper.valueToTree(context.getAffectedServices()));
		incidentJson.set("metricsSnapshot", objectMapper.valueToTree(context.getMetricsSnapshot()));
		incidentJson.set("timeline", objectMapper.valueToTree(context.getTimeline()));

		if (context.getLastUpdatedAt() != null) {
			incidentJson.put("lastUpdatedAt", context.getLastUpdatedAt().atOffset(ZoneOffset.UTC).toString());
		}

		return incidentJson;
	}

	private ObjectNode toServiceHealthJson(ServiceHealthSnapshot snapshot) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("projectId", snapshot.getProjectId());
		node.put("service", snapshot.getService());
		node.set("indicators", objectMapper.valueToTree(snapshot.getIndicators()));
		if (snapshot.getObservedAt() != null) {
			node.put("observedAt", snapshot.getObservedAt().atOffset(ZoneOffset.UTC).toString());
		}
		return node;
	}

	/**
	 * Internal helper holding prompt templates for RCA. Kept package-private so we
	 * don't leak prompt internals via public APIs.
	 */
	static final class InvestigateIncidentPrompts {

		private InvestigateIncidentPrompts() {
		}

		static String buildRcaPrompt(ObjectMapper objectMapper, IncidentContext context, ObjectNode evidence,
				int maxWords, String audience) {
			String evidenceJson;
			try {
				evidenceJson = objectMapper.writeValueAsString(evidence);
			}
			catch (Exception ex) {
				evidenceJson = "{}";
			}

			IncidentSummary summary = context.getSummary();
			String incidentId = summary != null ? summary.getId() : "";
			String title = summary != null ? summary.getTitle() : "";
			String service = summary != null ? summary.getService() : "";
			String severity = summary != null ? summary.getSeverity().name() : "";

			StringBuilder prompt = new StringBuilder();
			prompt.append(
					"You are a senior Site Reliability Engineer analysing an incident using data exported from Coroot. ")
					.append("You must produce a concise but rigorous root-cause analysis for a ")
					.append(audience)
					.append(" audience.\n\n");

			prompt.append("Hard constraints:\n")
					.append("- You MUST NOT invent metrics, timestamps, or services that are not present in the evidence JSON.\n")
					.append("- Distinguish clearly between observed facts and hypotheses.\n")
					.append("- If key evidence is missing, state that explicitly instead of guessing.\n")
					.append("- Keep the natural-language summary under ")
					.append(maxWords)
					.append(" words.\n\n");

			prompt.append("Incident meta:\n")
					.append("- Incident ID: ").append(incidentId).append("\n")
					.append("- Title: ").append(title).append("\n")
					.append("- Primary service: ").append(service).append("\n")
					.append("- Severity: ").append(severity).append("\n\n");

			prompt.append("Evidence JSON (single object):\n")
					.append(evidenceJson)
					.append("\n\n")
					.append("Write a natural-language incident report that includes:\n")
					.append("1) A short executive summary.\n")
					.append("2) What happened, in chronological order.\n")
					.append("3) Why it happened (root cause and contributing factors).\n")
					.append("4) Impact on SLOs and users.\n")
					.append("5) Recommended immediate and follow-up actions.\n")
					.append("6) Open questions / missing data.\n");

			return prompt.toString();
		}
	}

}

