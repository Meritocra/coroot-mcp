package com.meritocra.corootmcp.coroot;

import java.util.List;

public class DeploymentOverviewEntry {

	private final String projectId;

	private final String service;

	private final String cluster;

	private final String version;

	private final String status;

	private final String age;

	private final List<String> summary;

	public DeploymentOverviewEntry(String projectId, String service, String cluster, String version, String status,
			String age, List<String> summary) {
		this.projectId = projectId;
		this.service = service;
		this.cluster = cluster;
		this.version = version;
		this.status = status;
		this.age = age;
		this.summary = summary;
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

	public String getVersion() {
		return version;
	}

	public String getStatus() {
		return status;
	}

	public String getAge() {
		return age;
	}

	public List<String> getSummary() {
		return summary;
	}

}

