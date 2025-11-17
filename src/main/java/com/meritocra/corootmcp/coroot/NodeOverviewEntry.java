package com.meritocra.corootmcp.coroot;

import java.util.List;

public class NodeOverviewEntry {

	private final String projectId;

	private final String name;

	private final String cluster;

	private final String status;

	private final int applications;

	private final int instances;

	private final String uptime;

	private final List<String> privateIps;

	private final List<String> publicIps;

	private final Double cpuPercent;

	private final Double memoryPercent;

	private final Double networkPercent;

	private final Double diskPercent;

	public NodeOverviewEntry(String projectId, String name, String cluster, String status, int applications,
			int instances, String uptime, List<String> privateIps, List<String> publicIps, Double cpuPercent,
			Double memoryPercent, Double networkPercent, Double diskPercent) {
		this.projectId = projectId;
		this.name = name;
		this.cluster = cluster;
		this.status = status;
		this.applications = applications;
		this.instances = instances;
		this.uptime = uptime;
		this.privateIps = privateIps;
		this.publicIps = publicIps;
		this.cpuPercent = cpuPercent;
		this.memoryPercent = memoryPercent;
		this.networkPercent = networkPercent;
		this.diskPercent = diskPercent;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getName() {
		return name;
	}

	public String getCluster() {
		return cluster;
	}

	public String getStatus() {
		return status;
	}

	public int getApplications() {
		return applications;
	}

	public int getInstances() {
		return instances;
	}

	public String getUptime() {
		return uptime;
	}

	public List<String> getPrivateIps() {
		return privateIps;
	}

	public List<String> getPublicIps() {
		return publicIps;
	}

	public Double getCpuPercent() {
		return cpuPercent;
	}

	public Double getMemoryPercent() {
		return memoryPercent;
	}

	public Double getNetworkPercent() {
		return networkPercent;
	}

	public Double getDiskPercent() {
		return diskPercent;
	}

}

