package com.meritocra.corootmcp.coroot;

import java.time.Instant;
import java.util.Map;

public class ServiceHealthSnapshot {

	private final String projectId;

	private final String service;

	private final Map<String, Object> indicators;

	private final Instant observedAt;

	public ServiceHealthSnapshot(String projectId, String service, Map<String, Object> indicators, Instant observedAt) {
		this.projectId = projectId;
		this.service = service;
		this.indicators = indicators;
		this.observedAt = observedAt;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getService() {
		return service;
	}

	public Map<String, Object> getIndicators() {
		return indicators;
	}

	public Instant getObservedAt() {
		return observedAt;
	}
}

