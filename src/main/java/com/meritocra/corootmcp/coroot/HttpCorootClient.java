package com.meritocra.corootmcp.coroot;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.meritocra.corootmcp.config.CorootProperties;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * HTTP-backed implementation of {@link CorootClient} that talks to the Coroot
 * HTTP API.
 *
 * <p>
 * This client assumes the following Coroot API structure (aligned with the
 * Vitayou deployment and upstream docs):
 * </p>
 *
 * <ul>
 * <li>{@code GET /api/v1/projects/{projectId}/incidents} – list incidents for a
 * project.</li>
 * <li>{@code GET /api/v1/projects/{projectId}/incidents/{incidentId}} – fetch
 * detailed incident context.</li>
 * <li>{@code GET /api/v1/projects/{projectId}/services/{service}/health} –
 * fetch a compact health snapshot for a service.</li>
 * </ul>
 *
 * <p>
 * The Coroot API is expected to be secured with
 * {@code X-API-Key: <coroot.api-key>} as configured in {@link CorootProperties}.
 * </p>
 */
public class HttpCorootClient implements CorootClient {

	private final RestClient restClient;

	private final CorootProperties properties;

	public HttpCorootClient(CorootProperties properties) {
		Assert.notNull(properties, "corootProperties must not be null");
		Assert.notNull(properties.getApiUrl(), "coroot.api-url must be configured");
		if (!StringUtils.hasText(properties.getApiKey())) {
			throw new IllegalStateException("coroot.api-key (COROOT_API_KEY) must be configured");
		}

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(5_000);
		requestFactory.setReadTimeout(10_000);

		this.properties = properties;
		this.restClient = RestClient.builder()
				.requestFactory(requestFactory)
				.baseUrl(properties.getApiUrl().toString())
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader("X-API-Key", properties.getApiKey())
				.build();
	}

	@Override
	public IncidentContext getIncidentContext(String projectId, String incidentId) {
		Assert.hasText(projectId, "projectId must not be empty");
		Assert.hasText(incidentId, "incidentId must not be empty");

		Map<String, Object> response = restClient.get()
				.uri("/api/v1/projects/{projectId}/incidents/{incidentId}", projectId, incidentId)
				.retrieve()
				.body(Map.class);

		return toIncidentContext(response);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<IncidentSummary> listRecentIncidents(String projectId, IncidentSeverity minimumSeverity, int limit) {
		Assert.hasText(projectId, "projectId must not be empty");

		Map<String, Object> response = restClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/api/v1/projects/{projectId}/incidents")
						.queryParam("limit", limit)
						.build(projectId))
				.retrieve()
				.body(Map.class);

		List<Map<String, Object>> items = (List<Map<String, Object>>) response.getOrDefault("incidents",
				List.of());

		List<IncidentSummary> result = new ArrayList<>();
		for (Map<String, Object> item : items) {
			IncidentSummary summary = toIncidentSummary(item);
			if (summary.getSeverity().ordinal() >= minimumSeverity.ordinal()) {
				result.add(summary);
			}
		}

		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ServiceHealthSnapshot getServiceHealth(String projectId, String service) {
		Assert.hasText(projectId, "projectId must not be empty");
		Assert.hasText(service, "service must not be empty");

		Map<String, Object> response = restClient.get()
				.uri("/api/v1/projects/{projectId}/services/{service}/health", projectId, service)
				.retrieve()
				.body(Map.class);

		Map<String, Object> indicators = (Map<String, Object>) response.getOrDefault("indicators",
				Map.of());

		Instant observedAt = parseInstant(Objects.toString(response.get("observedAt"), null));

		return new ServiceHealthSnapshot(projectId, service, indicators, observedAt);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ProjectSummary> listProjects() {
		Map<String, Object> response = restClient.get()
				.uri("/api/user")
				.retrieve()
				.body(Map.class);

		List<Map<String, Object>> projects = (List<Map<String, Object>>) response.getOrDefault("projects", List.of());

		List<ProjectSummary> result = new ArrayList<>();
		for (Map<String, Object> project : projects) {
			String id = Objects.toString(project.get("id"), "");
			String name = Objects.toString(project.get("name"), id);
			result.add(new ProjectSummary(id, name));
		}

		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ApplicationOverviewEntry> listApplicationsOverview(String projectId) {
		Assert.hasText(projectId, "projectId must not be empty");

		Map<String, Object> response = restClient.get()
				.uri("/api/project/{projectId}/overview/applications", projectId)
				.retrieve()
				.body(Map.class);

		List<Map<String, Object>> applications = (List<Map<String, Object>>) response
				.getOrDefault("applications", List.of());

		List<ApplicationOverviewEntry> result = new ArrayList<>();
		for (Map<String, Object> app : applications) {
			Map<String, Object> id = (Map<String, Object>) app.getOrDefault("id", Map.of());
			String service = Objects.toString(id.getOrDefault("name", ""), "");

			String cluster = Objects.toString(app.get("cluster"), "");
			String category = Objects.toString(app.get("category"), "");
			String status = Objects.toString(app.get("status"), "");

			Map<String, Object> indicators = new HashMap<>();
			for (String key : List.of("errors", "latency", "upstreams", "instances", "restarts", "cpu", "memory",
					"disk_io_load", "disk_usage", "network", "dns", "logs")) {
				Map<String, Object> param = (Map<String, Object>) app.getOrDefault(key, Map.of());
				if (!param.isEmpty()) {
					Map<String, Object> summary = new HashMap<>();
					summary.put("status", Objects.toString(param.get("status"), ""));
					summary.put("value", Objects.toString(param.get("value"), ""));
					indicators.put(key, summary);
				}
			}

			result.add(new ApplicationOverviewEntry(projectId, service, cluster, category, status, indicators));
		}

		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<RiskOverviewEntry> listRisksOverview(String projectId) {
		Assert.hasText(projectId, "projectId must not be empty");

		Map<String, Object> response = restClient.get()
				.uri("/api/project/{projectId}/overview/risks", projectId)
				.retrieve()
				.body(Map.class);

		List<Map<String, Object>> risks = (List<Map<String, Object>>) response.getOrDefault("risks", List.of());

		List<RiskOverviewEntry> result = new ArrayList<>();
		for (Map<String, Object> risk : risks) {
			Map<String, Object> appId = (Map<String, Object>) risk.getOrDefault("application_id", Map.of());
			String service = Objects.toString(appId.getOrDefault("name", ""), "");

			String cluster = Objects.toString(risk.get("cluster"), "");
			String category = Objects.toString(risk.get("application_category"), "");
			String severity = Objects.toString(risk.get("severity"), "");
			String type = Objects.toString(risk.get("type"), "");

			Map<String, Object> exposure = (Map<String, Object>) risk.getOrDefault("exposure", Map.of());
			Map<String, Object> exposureCopy = new HashMap<>();
			if (!exposure.isEmpty()) {
				for (String key : List.of("ips", "ports", "node_port_services", "load_balancer_services")) {
					Object value = exposure.get(key);
					if (value != null) {
						exposureCopy.put(key, value);
					}
				}
			}

			Map<String, Object> availability = (Map<String, Object>) risk.getOrDefault("availability", Map.of());
			String availabilityDescription = Objects.toString(availability.get("description"), "");

			result.add(new RiskOverviewEntry(projectId, service, cluster, category, severity, type, exposureCopy,
					availabilityDescription));
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private IncidentContext toIncidentContext(Map<String, Object> payload) {
		IncidentSummary summary = toIncidentSummary((Map<String, Object>) payload.get("summary"));

		String suspectedRootCause = Objects.toString(payload.get("suspectedRootCause"), "");

		List<String> affectedServices = (List<String>) payload.getOrDefault("affectedServices",
				List.of());

		Map<String, Object> metricsSnapshot = (Map<String, Object>) payload
				.getOrDefault("metricsSnapshot", Map.of());

		List<String> timeline = (List<String>) payload.getOrDefault("timeline",
				List.of());

		Instant lastUpdatedAt = parseInstant(Objects.toString(payload.get("lastUpdatedAt"), null));

		return new IncidentContext(summary, suspectedRootCause, affectedServices, metricsSnapshot, timeline,
				lastUpdatedAt);
	}

	private IncidentSummary toIncidentSummary(Map<String, Object> payload) {
		String id = Objects.toString(payload.get("id"), "");
		String title = Objects.toString(payload.get("title"), "");
		String service = Objects.toString(payload.get("service"), "");
		String severityText = Objects.toString(payload.get("severity"), "INFO");

		IncidentSeverity severity;
		try {
			severity = IncidentSeverity.valueOf(severityText.toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			severity = IncidentSeverity.INFO;
		}

		Instant startedAt = parseInstant(Objects.toString(payload.get("startedAt"), null));
		Instant endedAt = parseInstant(Objects.toString(payload.get("endedAt"), null));

		return new IncidentSummary(id, title, severity, service, startedAt, endedAt);
	}

	private Instant parseInstant(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			// ISO-8601 with offset, e.g. 2025-11-14T10:00:00Z
			return OffsetDateTime.parse(value).toInstant();
		}
		catch (Exception ex) {
			// Fallback: epoch seconds
			try {
				long epochSeconds = Long.parseLong(value);
				return Instant.ofEpochSecond(epochSeconds);
			}
			catch (Exception ignored) {
				return Instant.now().atOffset(ZoneOffset.UTC).toInstant();
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<NodeOverviewEntry> listNodesOverview(String projectId) {
		Assert.hasText(projectId, "projectId must not be empty");

		Map<String, Object> response = restClient.get()
				.uri("/api/project/{projectId}/overview/nodes", projectId)
				.retrieve()
				.body(Map.class);

		List<Map<String, Object>> nodes = (List<Map<String, Object>>) response.getOrDefault("nodes", List.of());

		List<NodeOverviewEntry> result = new ArrayList<>();
		for (Map<String, Object> node : nodes) {
			String name = Objects.toString(node.get("name"), "");
			String cluster = Objects.toString(node.get("cluster"), "");
			String status = Objects.toString(node.get("status"), "");

			int applications = toInt(node.get("applications"));
			int instances = toInt(node.get("instances"));
			String uptime = Objects.toString(node.get("uptime"), "");

			List<String> privateIps = (List<String>) node.getOrDefault("private_ips", List.of());
			List<String> publicIps = (List<String>) node.getOrDefault("public_ips", List.of());

			Double cpu = toDouble(node.get("cpu"));
			Double memory = toDouble(node.get("memory"));
			Double network = toDouble(node.get("network"));
			Double disk = toDouble(node.get("disk"));

			result.add(new NodeOverviewEntry(projectId, name, cluster, status, applications, instances, uptime,
					privateIps, publicIps, cpu, memory, network, disk));
		}

		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DeploymentOverviewEntry> listDeploymentsOverview(String projectId) {
		Assert.hasText(projectId, "projectId must not be empty");

		Map<String, Object> response = restClient.get()
				.uri("/api/project/{projectId}/overview/deployments", projectId)
				.retrieve()
				.body(Map.class);

		List<Map<String, Object>> deployments = (List<Map<String, Object>>) response.getOrDefault("deployments",
				List.of());

		List<DeploymentOverviewEntry> result = new ArrayList<>();
		for (Map<String, Object> deployment : deployments) {
			Map<String, Object> appId = (Map<String, Object>) deployment.getOrDefault("application_id", Map.of());

			String service = Objects.toString(appId.getOrDefault("name", ""), "");
			String cluster = Objects.toString(appId.getOrDefault("cluster_id", ""), "");

			String version = Objects.toString(deployment.get("version"), "");
			String status = Objects.toString(deployment.get("status"), "");
			String age = Objects.toString(deployment.get("age"), "");

			List<Map<String, Object>> events = (List<Map<String, Object>>) deployment.getOrDefault("events", List.of());
			List<String> summary = new ArrayList<>();
			for (Map<String, Object> event : events) {
				String message = Objects.toString(event.get("message"), "");
				String eventStatus = Objects.toString(event.get("status"), "");
				if (!message.isEmpty()) {
					if (!eventStatus.isEmpty()) {
						summary.add(eventStatus + ": " + message);
					}
					else {
						summary.add(message);
					}
				}
			}

			result.add(new DeploymentOverviewEntry(projectId, service, cluster, version, status, age, summary));
		}

		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> getTracesOverview(String projectId, String query) {
		Assert.hasText(projectId, "projectId must not be empty");

		Map<String, Object> response = restClient.get()
				.uri(uriBuilder -> {
					var builder = uriBuilder.path("/api/project/{projectId}/overview/traces");
					if (StringUtils.hasText(query)) {
						builder.queryParam("query", query);
					}
					return builder.build(projectId);
				})
				.retrieve()
				.body(Map.class);

		if (response == null) {
			return Map.of();
		}

		return response;
	}

	private int toInt(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		try {
			return value != null ? Integer.parseInt(value.toString()) : 0;
		}
		catch (NumberFormatException ex) {
			return 0;
		}
	}

	private Double toDouble(Object value) {
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		try {
			return value != null ? Double.parseDouble(value.toString()) : null;
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}
}
