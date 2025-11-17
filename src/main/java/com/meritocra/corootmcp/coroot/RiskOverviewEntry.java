package com.meritocra.corootmcp.coroot;

import java.util.List;
import java.util.Map;

public class RiskOverviewEntry {

	private final String projectId;

	private final String service;

	private final String cluster;

	private final String category;

	private final String severity;

	private final String type;

	private final Map<String, Object> exposure;

	private final String availability;

	public RiskOverviewEntry(String projectId, String service, String cluster, String category, String severity,
			String type, Map<String, Object> exposure, String availability) {
		this.projectId = projectId;
		this.service = service;
		this.cluster = cluster;
		this.category = category;
		this.severity = severity;
		this.type = type;
		this.exposure = exposure;
		this.availability = availability;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getService() {
		return service;
	}

	public String getCluster() {
		return cluster;
	}

	public String getCategory() {
		return category;
	}

	public String getSeverity() {
		return severity;
	}

	public String getType() {
		return type;
	}

	public Map<String, Object> getExposure() {
		return exposure;
	}

	public String getAvailability() {
		return availability;
	}

}

