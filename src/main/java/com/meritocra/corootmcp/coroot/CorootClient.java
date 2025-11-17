package com.meritocra.corootmcp.coroot;

import java.util.List;
import java.util.Map;

public interface CorootClient {

	IncidentContext getIncidentContext(String projectId, String incidentId);

	List<IncidentSummary> listRecentIncidents(String projectId, IncidentSeverity minimumSeverity, int limit);

	ServiceHealthSnapshot getServiceHealth(String projectId, String service);

	List<ProjectSummary> listProjects();

	List<ApplicationOverviewEntry> listApplicationsOverview(String projectId);

	List<RiskOverviewEntry> listRisksOverview(String projectId);

	List<NodeOverviewEntry> listNodesOverview(String projectId);

	List<DeploymentOverviewEntry> listDeploymentsOverview(String projectId);

	Map<String, Object> getTracesOverview(String projectId, String query);

	Map<String, Object> getLogsOverview(String projectId, String query);

	Map<String, Object> getApplicationTracing(String projectId, String applicationId, int windowMinutes);

	Map<String, Object> getApplicationLogs(String projectId, String applicationId, int windowMinutes, int maxEntries);

	Map<String, Object> getCostsOverview(String projectId);

	Map<String, Object> getSloOverview(String projectId);

}
