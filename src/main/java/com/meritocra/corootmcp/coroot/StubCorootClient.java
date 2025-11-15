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
}
