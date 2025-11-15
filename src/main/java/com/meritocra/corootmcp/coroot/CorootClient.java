package com.meritocra.corootmcp.coroot;

import java.util.List;

public interface CorootClient {

	IncidentContext getIncidentContext(String projectId, String incidentId);

	List<IncidentSummary> listRecentIncidents(String projectId, IncidentSeverity minimumSeverity, int limit);

	ServiceHealthSnapshot getServiceHealth(String projectId, String service);
}

