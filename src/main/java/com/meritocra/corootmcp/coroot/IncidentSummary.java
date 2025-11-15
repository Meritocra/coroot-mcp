package com.meritocra.corootmcp.coroot;

import java.time.Instant;

public class IncidentSummary {

	private final String id;

	private final String title;

	private final IncidentSeverity severity;

	private final String service;

	private final Instant startedAt;

	private final Instant endedAt;

	public IncidentSummary(String id, String title, IncidentSeverity severity, String service, Instant startedAt,
			Instant endedAt) {
		this.id = id;
		this.title = title;
		this.severity = severity;
		this.service = service;
		this.startedAt = startedAt;
		this.endedAt = endedAt;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public IncidentSeverity getSeverity() {
		return severity;
	}

	public String getService() {
		return service;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getEndedAt() {
		return endedAt;
	}
}

