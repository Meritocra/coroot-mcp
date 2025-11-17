package com.meritocra.corootmcp.coroot;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
}
