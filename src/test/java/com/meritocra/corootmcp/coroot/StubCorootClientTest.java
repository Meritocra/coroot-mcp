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
}
