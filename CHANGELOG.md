# Changelog

All notable changes to this project will be documented in this file.

The format is inspired by [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
