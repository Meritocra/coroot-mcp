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

}
