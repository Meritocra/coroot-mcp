package com.meritocra.corootmcp.coroot;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class StubCorootClient implements CorootClient {

	@Override
	public IncidentContext getIncidentContext(String projectId, String incidentId) {
		IncidentSummary summary = new IncidentSummary(incidentId, "Checkout latency spike",
				IncidentSeverity.CRITICAL, "checkout-service",
				Instant.now().minusSeconds(900), Instant.now().minusSeconds(300));

		return new IncidentContext(summary,
				"Recent deployment of checkout-service increased database query latency and saturated the connection pool.",
				List.of("checkout-service", "payments-service", "postgresql"),
				Map.of("p95_latency_ms", 1200, "error_rate_percent", 2.3, "qps", 350),
				List.of(
						"Deployment of checkout-service version 1.24.0",
						"Database CPU utilization sustained above 85%",
						"Prometheus alert fired: CheckoutHighLatency",
						"Automatic rollback initiated but did not complete"),
				Instant.now().minusSeconds(120));
	}

	@Override
	public List<IncidentSummary> listRecentIncidents(String projectId, IncidentSeverity minimumSeverity, int limit) {
		IncidentSummary first = new IncidentSummary("inc-1", "Checkout latency spike",
				IncidentSeverity.CRITICAL, "checkout-service",
				Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));
		IncidentSummary second = new IncidentSummary("inc-2", "Intermittent 5xx on catalog",
				IncidentSeverity.WARNING, "catalog-service",
				Instant.now().minusSeconds(10800), Instant.now().minusSeconds(5400));

		return List.of(first, second);
	}

	@Override
	public ServiceHealthSnapshot getServiceHealth(String projectId, String service) {
		return new ServiceHealthSnapshot(projectId, service,
				Map.of("p95_latency_ms", 350, "error_rate_percent", 0.2, "saturation_percent", 63),
				Instant.now());
	}

	@Override
	public List<ProjectSummary> listProjects() {
		return List.of(new ProjectSummary("production", "Production"));
	}

	@Override
	public List<ApplicationOverviewEntry> listApplicationsOverview(String projectId) {
		ApplicationOverviewEntry first = new ApplicationOverviewEntry(projectId, "checkout-service", "cluster-1",
				"web", "CRITICAL",
				Map.of("errors", Map.of("status", "CRITICAL", "value", "2.3%"), "latency",
						Map.of("status", "CRITICAL", "value", "1.2s")));

		ApplicationOverviewEntry second = new ApplicationOverviewEntry(projectId, "catalog-service", "cluster-1",
				"web", "WARNING",
				Map.of("errors", Map.of("status", "WARNING", "value", "0.5%"), "latency",
						Map.of("status", "OK", "value", "250ms")));

		return List.of(first, second);
	}

	@Override
	public List<RiskOverviewEntry> listRisksOverview(String projectId) {
		RiskOverviewEntry risk = new RiskOverviewEntry(projectId, "checkout-service", "cluster-1", "web", "CRITICAL",
				"publicly_exposed",
				Map.of("ips", List.of("203.0.113.10"), "ports", List.of("443")), "Single AZ deployment");

		return List.of(risk);
	}

	@Override
	public List<NodeOverviewEntry> listNodesOverview(String projectId) {
		NodeOverviewEntry node = new NodeOverviewEntry(projectId, "node-1", "cluster-1", "OK", 5, 10, "3d",
				List.of("10.0.0.10"), List.of("203.0.113.10"), 55.0, 62.0, 30.0, 40.0);

		return List.of(node);
	}

	@Override
	public List<DeploymentOverviewEntry> listDeploymentsOverview(String projectId) {
		DeploymentOverviewEntry deployment = new DeploymentOverviewEntry(projectId, "checkout-service", "cluster-1",
				"1.24.0", "COMPLETED", "2h", List.of("COMPLETED: rollout finished"));

		return List.of(deployment);
	}

	@Override
	public Map<String, Object> getTracesOverview(String projectId, String query) {
		return Map.of(
				"projectId", projectId,
				"summary", Map.of("requestsPerSecond", 72, "errorRatePercent", 1.2),
				"topEndpoints", List.of(
						Map.of("service", "checkout-service", "p95LatencyMs", 240, "errorRatePercent", 2.3),
						Map.of("service", "catalog-service", "p95LatencyMs", 120, "errorRatePercent", 0.4)));
	}
}
