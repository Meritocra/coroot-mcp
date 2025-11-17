package com.meritocra.corootmcp.coroot;

import java.util.Map;

public class ApplicationOverviewEntry {

	private final String projectId;

	private final String service;

	private final String cluster;

	private final String category;

	private final String status;

	private final Map<String, Object> indicators;

	public ApplicationOverviewEntry(String projectId, String service, String cluster, String category, String status,
			Map<String, Object> indicators) {
		this.projectId = projectId;
		this.service = service;
		this.cluster = cluster;
		this.category = category;
		this.status = status;
		this.indicators = indicators;
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

	public String getStatus() {
		return status;
	}

	public Map<String, Object> getIndicators() {
		return indicators;
	}

}

