package com.meritocra.corootmcp.coroot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class StubCorootClientTest {

	private final StubCorootClient client = new StubCorootClient();

	@Test
	void givenIncidentId_whenFetchingIncidentContext_thenReturnsSummaryAndTimeline() {
		// when
		IncidentContext context = client.getIncidentContext("project", "incident-1");

		// then
		assertThat(context.getSummary().getId()).isEqualTo("incident-1");
		assertThat(context.getSuspectedRootCause()).isNotBlank();
		assertThat(context.getAffectedServices()).isNotEmpty();
		assertThat(context.getMetricsSnapshot()).isNotEmpty();
		assertThat(context.getTimeline()).isNotEmpty();
	}

	@Test
	void givenProject_whenListingRecentIncidents_thenReturnsNonEmptyList() {
		// when
		List<IncidentSummary> incidents = client.listRecentIncidents("project", IncidentSeverity.WARNING, 10);

		// then
		assertThat(incidents).isNotEmpty();
		assertThat(incidents.get(0).getId()).isNotBlank();
	}

	@Test
	void givenService_whenFetchingServiceHealth_thenReturnsSnapshotWithIndicators() {
		// when
		ServiceHealthSnapshot snapshot = client.getServiceHealth("project", "checkout-service");

		// then
		assertThat(snapshot.getProjectId()).isEqualTo("project");
		assertThat(snapshot.getService()).isEqualTo("checkout-service");
		assertThat(snapshot.getIndicators()).isNotEmpty();
		assertThat(snapshot.getObservedAt()).isNotNull();
	}

	@Test
	void givenClient_whenListingProjects_thenReturnsAtLeastOneProject() {
		// when
		List<ProjectSummary> projects = client.listProjects();

		// then
		assertThat(projects).isNotEmpty();
		assertThat(projects.get(0).getId()).isNotBlank();
	}

	@Test
	void givenProject_whenListingApplicationsOverview_thenReturnsEntries() {
		// when
		List<ApplicationOverviewEntry> apps = client.listApplicationsOverview("production");

		// then
		assertThat(apps).isNotEmpty();
		assertThat(apps.get(0).getService()).isNotBlank();
		assertThat(apps.get(0).getIndicators()).isNotEmpty();
	}

	@Test
	void givenProject_whenListingRisksOverview_thenReturnsRisks() {
		// when
		List<RiskOverviewEntry> risks = client.listRisksOverview("production");

		// then
		assertThat(risks).isNotEmpty();
		assertThat(risks.get(0).getService()).isNotBlank();
		assertThat(risks.get(0).getSeverity()).isNotBlank();
	}

	@Test
	void givenProject_whenListingNodesOverview_thenReturnsNodes() {
		// when
		List<NodeOverviewEntry> nodes = client.listNodesOverview("production");

		// then
		assertThat(nodes).isNotEmpty();
		assertThat(nodes.get(0).getName()).isNotBlank();
	}

	@Test
	void givenProject_whenListingDeploymentsOverview_thenReturnsDeployments() {
		// when
		List<DeploymentOverviewEntry> deployments = client.listDeploymentsOverview("production");

		// then
		assertThat(deployments).isNotEmpty();
		assertThat(deployments.get(0).getService()).isNotBlank();
	}
}
