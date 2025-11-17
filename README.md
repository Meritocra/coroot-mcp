# coroot-mcp

[![CI](https://github.com/Meritocra/coroot-mcp/actions/workflows/ci.yml/badge.svg)](https://github.com/Meritocra/coroot-mcp/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-blue.svg)](pom.xml)

[Docs](#what-this-mcp-server-does) · [Coroot](https://coroot.com) · [Changelog](CHANGELOG.md) · [Contributing](CONTRIBUTING.md)

Coroot MCP is a Model Context Protocol (MCP) server that turns your [Coroot](https://github.com/coroot/coroot) observability stack into a set of well-typed tools an LLM assistant can call for root-cause analysis.

This is a community-maintained integration and is not an official Coroot product or endorsement.

It is implemented as a [Spring Boot 3](https://spring.io/projects/spring-boot) / [Spring AI 1.1.x](https://spring.io/projects/spring-ai) application and exposes a JSON-RPC 2.0 MCP endpoint over HTTP at `/mcp`.

The project is licensed under the MIT License.

## Quick start (no Coroot required)

You can try the MCP server without a Coroot instance by enabling the built-in stub client profile. This returns synthetic incidents and health snapshots that are good enough for testing tool wiring.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=stub-coroot
```

The MCP JSON-RPC endpoint will be available at:

- `POST http://localhost:8080/mcp`

## Quick start (real Coroot)

To point `coroot-mcp` at a real Coroot instance:

1. Create a local `.env.local` file in this repo (gitignored) with:

   ```bash
   COROOT_API_URL=https://coroot.your-company.com
   COROOT_API_KEY=<your Coroot API key>
   COROOT_DEFAULT_PROJECT_ID=production

   OPENAI_API_KEY=<your OpenAI/OpenAI-compatible key>
   OPENAI_MODEL=gpt-4.1-mini
   ```

2. Start the server:

   ```bash
   set -a
   source .env.local
   set +a

   ./mvnw spring-boot:run
   ```

3. Verify it is running:

   ```bash
   curl -s http://localhost:8080/actuator/health
   curl -s http://localhost:8080/mcp \
     -H 'Content-Type: application/json' \
     -d '{"jsonrpc":"2.0","id":"init-1","method":"initialize","params":{}}'
   ```

4. Point your MCP-aware client (Codex, Claude, Toolhive, etc.) at `http://localhost:8080/mcp` and start using tools like `list_recent_incidents` and `investigate_incident`.

## Using with AI coding agents

This project speaks MCP over HTTP. Any MCP-aware coding assistant can talk to it once you point the client at the `/mcp` endpoint.

### Codex CLI (local MCP)

Assuming `coroot-mcp` runs on `http://localhost:8080/mcp`, add an MCP server entry in your Codex configuration, for example:

```toml
[mcp_servers.coroot-mcp]
command = "npx"
args = ["-y", "mcp-remote", "http://localhost:8080/mcp", "--allow-http", "--transport", "http-first"]
```

Restart Codex CLI and list MCP servers to confirm that `coroot-mcp` is available.

### Claude / Claude Code (HTTP MCP)

If you use a Claude-based environment that supports HTTP MCP servers, configure a new MCP server named `coroot-mcp` with:

- Type: HTTP
- URL: `http://localhost:8080/mcp`

You can then call `list_recent_incidents` and `summarize_incident_root_cause` from within that environment.

## Configuration examples (JSON)

Some tools and IDEs prefer JSON-based MCP configuration files. The snippets below mirror what many MCP-aware agents expect as of late 2025; adjust paths and secrets to your environment.

### Generic `.mcp.json` (project-level)

You can keep a project-scoped MCP configuration in `.mcp.json` at the root of your repo:

```jsonc
{
  "mcpServers": {
    "coroot-mcp": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

Some clients also support an `env` block here; if yours does, you can document expected variables:

```jsonc
{
  "mcpServers": {
    "coroot-mcp": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "env": {
        "OPENAI_API_KEY": "sk-…",
        "COROOT_API_URL": "https://coroot.your-company.com",
        "COROOT_DEFAULT_PROJECT_ID": "production"
      }
    }
  }
}
```

### Claude JSON config (`~/.claude.json`)

Claude CLI and Claude Desktop both read MCP servers from JSON config files. A minimal global configuration looks like:

```jsonc
{
  "mcpServers": {
    "coroot-mcp": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

If you prefer to scope `coroot-mcp` to a single folder, add the same `mcpServers` block under the project entry in `~/.claude.json` or in a per-project settings file (see Claude docs for the latest supported locations).

## What this MCP server does

- Exposes Coroot [incidents](https://docs.coroot.com/alerting/incidents/) and [application health summaries](https://github.com/coroot/coroot#application-health-summary) as MCP tools that an AI assistant can call.
- Provides natural-language root-cause summaries grounded in Coroot data via [AI-powered Root Cause Analysis](https://docs.coroot.com/ai/) concepts, implemented here with Spring AI.
- Returns compact JSON payloads for incidents and summaries so downstream tooling (postmortems, executive reports, dashboards, etc.) can build on top.

### Use cases

- Triage Coroot incidents from an AI-enabled IDE or CLI (Codex, Claude, etc.) without leaving your editor.
- Generate clear, executive-friendly summaries of incidents for postmortems, status updates, or incident review meetings.
- Enrich runbooks, dashboards, or other internal tools with machine-readable incident context from Coroot.

## Available tools

The MCP server exposes tools via `tools/list` and `tools/call`.

| MCP tool                    | What it does                                                                                                      | Coroot feature                                                                 |
|----------------------------|--------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| `list_projects`            | Returns the Coroot projects the current API key can access.                                                       | [Projects](https://docs.coroot.com/getting-started/projects/) visible in the Coroot UI. |
| `list_recent_incidents`    | Lists recent incidents with id, title, service, severity, and timestamps, with optional filters.                  | [Incidents](https://docs.coroot.com/alerting/incidents/).                     |
| `get_incident_details`     | Returns detailed incident context (summary, suspected root cause, affected services, metrics, timeline).          | Incident detail view in the Coroot UI.                                        |
| `summarize_incident_root_cause` | Generates a natural-language explanation of an incident’s likely root cause, blast radius, and remediation steps. | [AI-powered Root Cause Analysis](https://docs.coroot.com/ai/).                |
| `investigate_incident`     | Performs a full incident investigation and returns an RCA summary plus a structured JSON payload.                | Incidents, AI RCA, SLOs, and related overviews in Coroot.                     |
| `get_applications_overview`| Returns an overview of application health for a project (per‑service status and key indicators).                  | [Application Health Summary](https://github.com/coroot/coroot#application-health-summary). |
| `get_service_health`       | Returns a compact health snapshot for a single service, including key indicators.                                 | Per‑service health indicators; see [Service inspections](https://docs.coroot.com/inspections/services/). |
| `get_nodes_overview`       | Returns an overview of node health for a project (CPU, memory, network, disk, private/public IPs).               | Nodes section of the [Overview](https://docs.coroot.com/overview).           |
| `get_deployments_overview` | Returns recent deployments, including service, version, status, age, and a concise event summary.                | [Deployment tracking](https://docs.coroot.com/inspections/deployment-tracking). |
| `get_risks_overview`       | Returns a summary of risks for a project, including exposure (IPs, ports) and availability information.          | [Risk overview](https://docs.coroot.com/risks/overview).                      |
| `get_traces_overview`      | Returns a tracing overview for a project, optionally filtered by a query string (service, endpoint, etc.).       | [Tracing overview](https://docs.coroot.com/tracing/overview).                |
| `get_application_traces`   | Returns recent spans for a single application over a bounded time window, summarised for analysis.               | Application tracing view in Coroot’s tracing UI.                              |
| `get_logs_overview`        | Returns a logs overview for a project, optionally filtered by a log query string.                                | [Logs overview](https://docs.coroot.com/logs/overview).                      |
| `get_application_logs`     | Returns recent log entries for a single application over a bounded time window, with a severity breakdown.       | Application logs view in Coroot’s logs UI.                                    |
| `get_costs_overview`       | Returns a cost overview for nodes and applications in a project.                                                 | [Costs overview](https://docs.coroot.com/costs/overview).                    |
| `get_slo_overview`         | Returns SLO availability and latency objectives at project and per‑service level.                               | [SLOs](https://docs.coroot.com/inspections/slo/).                            |

All tools are read-only and safe to expose to assistants by default.

## Design

- Configuration is supplied via environment variables (for example `OPENAI_API_KEY`, `COROOT_API_URL`, `COROOT_DEFAULT_PROJECT_ID`).
- The HTTP/MCP layer is separated from the Coroot client and domain model so tools stay small and focused.
- Tools are read-only and side-effect free, returning explicit JSON schemas.
- The MCP server is stateless; Coroot and the LLM are external backing services.

## Requirements

- Java 21+
- Maven 3.9+
- A running Coroot instance (for production use)
- An OpenAI-compatible API key (for Spring AI)

## Configuration

Environment variables (12-factor style):

- `OPENAI_API_KEY` – API key used by Spring AI to talk to the OpenAI-compatible model.
- `OPENAI_MODEL` – optional, defaults to `gpt-4.1-mini`.
- `COROOT_API_URL` – base URL of your Coroot instance, defaults to `https://coroot.vitayou.io`.
- `COROOT_DEFAULT_PROJECT_ID` – default Coroot project ID when a tool call omits it (e.g. `production`).
- `MCP_AUTH_TOKEN` – optional bearer token required on `/mcp` when set. Clients must send `Authorization: Bearer <token>`.

These map to Spring Boot configuration in `src/main/resources/application.properties`.

## Running locally against Coroot (dev)

```bash
export OPENAI_API_KEY=sk-...
export MCP_AUTH_TOKEN=some-secret-token    # optional
export COROOT_API_URL=https://coroot.your-company.com
export COROOT_DEFAULT_PROJECT_ID=production

./mvnw spring-boot:run
```

The MCP JSON-RPC endpoint will be available at:

- `POST http://localhost:8080/mcp`

If `MCP_AUTH_TOKEN` is set, clients must send:

- `Authorization: Bearer <token>`

## Docker / container image

### Using a published image

Once an image is published to a registry such as GitHub Container Registry, you can run it directly. For example, assuming:

- Image: `ghcr.io/meritocra/coroot-mcp:0.2.0`

Run against a real Coroot instance:

```bash
docker run --rm -p 8080:8080 \
  -e OPENAI_API_KEY=sk-... \
  -e MCP_AUTH_TOKEN=some-secret-token \
  -e COROOT_API_URL=https://coroot.your-company.com \
  -e COROOT_DEFAULT_PROJECT_ID=production \
  ghcr.io/meritocra/coroot-mcp:0.2.0
```

Run in stub mode (no Coroot required):

```bash
docker run --rm -p 8080:8080 \
  -e MCP_AUTH_TOKEN=some-secret-token \
  -e SPRING_PROFILES_ACTIVE=stub-coroot \
  ghcr.io/meritocra/coroot-mcp:0.2.0
```

### Building the image locally

Build a container image using the provided multi-stage `Dockerfile`:

```bash
docker build -t coroot-mcp:latest .
```

Run it:

```bash
docker run --rm -p 8080:8080 \
  -e OPENAI_API_KEY=sk-... \
  -e COROOT_API_URL=https://coroot.your-company.com \
  -e COROOT_DEFAULT_PROJECT_ID=production \
  coroot-mcp:latest
```

It is recommended to set `JAVA_OPTS` for resource limits, for example:

```bash
docker run --rm -p 8080:8080 \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  -e OPENAI_API_KEY=sk-... \
  -e COROOT_API_URL=https://coroot.your-company.com \
  -e COROOT_DEFAULT_PROJECT_ID=production \
  coroot-mcp:latest
```

### Docker Compose

For local experiments, you can also use the provided `docker-compose.yml`:

```bash
docker compose up --build
```

By default this starts `coroot-mcp` with the `stub-coroot` profile and exposes the MCP endpoint on:

- `POST http://localhost:8080/mcp`

## MCP manifest

The MCP manifest is defined in `mcp.json`. It declares:

- Server name and description.
- HTTP transport pointing to `http://localhost:8080/mcp`.
- Declared secrets: `OPENAI_API_KEY`, `COROOT_API_KEY`.
- Declared non-secret environment variables: `OPENAI_MODEL`, `COROOT_API_URL`, `COROOT_DEFAULT_PROJECT_ID`.

MCP-compatible clients can either:

- Load the manifest from disk (e.g. `mcp.json` in a project directory), or
- Be configured directly with the MCP HTTP endpoint URL (`http://localhost:8080/mcp`).

## Using with Toolhive (example)

[Toolhive](https://github.com/stacklok/toolhive) is a multi-server MCP manager that can run in your cluster or on the desktop.

1. Build and run the server (locally or in a container).
2. Ensure `mcp.json` is accessible to Toolhive (either via local file path or by copying its contents into Toolhive’s MCP configuration).
3. In Toolhive, add a new MCP server and point it at:
   - Manifest: `mcp.json`
   - Transport URL: `http://localhost:8080/mcp`
4. From Toolhive’s assistant, ask questions like:
   - “List the latest critical incidents in production.”
   - “Explain the root cause of incident `inc-1` and suggest next steps.”

Toolhive will call the MCP tools under the hood and present their outputs.

## Links

- Coroot website: https://coroot.com
- Coroot on GitHub: https://github.com/coroot/coroot
- MCP spec and tooling: https://modelcontextprotocol.io
- Toolhive on GitHub: https://github.com/stacklok/toolhive

## Releasing & publishing images

This repository includes a tag-based release workflow:

- Pushing a tag like `v0.2.0` to GitHub triggers `.github/workflows/release.yml`.
- The workflow runs the test suite, builds the Spring Boot JAR, and builds a container image.
- The image is pushed to GitHub Container Registry as `ghcr.io/<owner>/coroot-mcp:<tag-without-v>`.

For example, tag `v0.2.0` produces:

- `ghcr.io/meritocra/coroot-mcp:0.2.0`

## License & attribution

- This repository (`coroot-mcp`) is licensed under the MIT License (see `LICENSE`).
- It integrates with Coroot Community Edition, which is licensed under Apache-2.0:
  - Coroot repo: https://github.com/coroot/coroot
  - License: Apache License, Version 2.0.
- This MCP server is a community-maintained integration and is not an official Coroot product or endorsement.
