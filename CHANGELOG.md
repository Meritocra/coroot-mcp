# Changelog

All notable changes to this project will be documented in this file.

The format is inspired by [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2025-11-17

### Added

- `list_projects` tool backed by the Coroot user API, returning projects visible to the current API key.
- `get_incident_details` tool exposing detailed incident context (suspected root cause, affected services, metrics snapshot, timeline).
- `get_applications_overview` tool backed by `/api/project/{project}/overview/applications` (Application Health Summary).
- `get_service_health` tool backed by `/api/v1/projects/{projectId}/services/{service}/health`.
- `get_risks_overview` tool backed by `/api/project/{project}/overview/risks`.
- `get_nodes_overview` tool backed by `/api/project/{project}/overview/nodes`.
- `get_deployments_overview` tool backed by `/api/project/{project}/overview/deployments`.
- `get_traces_overview` tool backed by `/api/project/{project}/overview/traces`.
 - `get_logs_overview` tool backed by `/api/project/{project}/overview/logs`.
 - `get_costs_overview` tool backed by `/api/project/{project}/overview/costs`.
 - `get_slo_overview` tool backed by `/api/project/{project}/inspections`.
- Guard beans that fail fast when required Coroot or OpenAI configuration is missing outside the `stub-coroot` profile.
- README updates with modern quickstart, AI agent usage, Toolhive example, and links to relevant Coroot documentation.

## [0.1.0] - 2025-11-16

### Added

- HTTP MCP endpoint at `/mcp` with `initialize`, `tools/list`, and `tools/call` methods.
- `list_recent_incidents` tool returning Coroot incidents as compact JSON.
- `summarize_incident_root_cause` tool providing a natural-language summary and structured incident context.
- Coroot client abstraction with HTTP implementation and a stub implementation for local testing.
- Spring Boot 3 / Spring AI integration for summarization.
- Dockerfile and `docker-compose.yml` for running the MCP server in a container.
- Test suite with unit and integration tests plus JaCoCo coverage reporting.
- Health and readiness endpoints exposed via Spring Boot Actuator.
- HTTP timeouts for the Coroot HTTP client.
- Optional bearer token authentication for `/mcp` via `MCP_AUTH_TOKEN`.
