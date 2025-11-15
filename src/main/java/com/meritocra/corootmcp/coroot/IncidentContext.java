package com.meritocra.corootmcp.coroot;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class IncidentContext {

	private final IncidentSummary summary;

	private final String suspectedRootCause;

	private final List<String> affectedServices;

	private final Map<String, Object> metricsSnapshot;

	private final List<String> timeline;

	private final Instant lastUpdatedAt;

	public IncidentContext(IncidentSummary summary, String suspectedRootCause, List<String> affectedServices,
			Map<String, Object> metricsSnapshot, List<String> timeline, Instant lastUpdatedAt) {
		this.summary = summary;
		this.suspectedRootCause = suspectedRootCause;
		this.affectedServices = affectedServices;
		this.metricsSnapshot = metricsSnapshot;
		this.timeline = timeline;
		this.lastUpdatedAt = lastUpdatedAt;
	}

	public IncidentSummary getSummary() {
		return summary;
	}

	public String getSuspectedRootCause() {
		return suspectedRootCause;
	}

	public List<String> getAffectedServices() {
		return affectedServices;
	}

	public Map<String, Object> getMetricsSnapshot() {
		return metricsSnapshot;
	}

	public List<String> getTimeline() {
		return timeline;
	}

	public Instant getLastUpdatedAt() {
		return lastUpdatedAt;
	}
}

