# coroot-mcp

[![CI](https://github.com/Meritocra/coroot-mcp/actions/workflows/ci.yml/badge.svg)](https://github.com/Meritocra/coroot-mcp/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-blue.svg)](pom.xml)

Coroot MCP is a Model Context Protocol (MCP) server that turns your [Coroot](https://github.com/coroot/coroot) observability stack into a set of well-typed tools an LLM assistant can call for root-cause analysis.

It is implemented as a Spring Boot 3 / Spring AI 1.1.x application and exposes a JSON-RPC 2.0 MCP endpoint over HTTP at `/mcp`.

The project is licensed under the MIT License.

## Quick start (no Coroot required)

You can try the MCP server without a Coroot instance by enabling the built-in stub client profile. This returns synthetic incidents and health snapshots that are good enough for testing tool wiring.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=stub-coroot
```

The MCP JSON-RPC endpoint will be available at:

- `POST http://localhost:8080/mcp`

From another terminal, you can send a basic `initialize` request:

```bash
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  # -H 'Authorization: Bearer some-secret-token' \  # if MCP_AUTH_TOKEN is set
  -d '{
    "jsonrpc": "2.0",
    "id": "init-1",
    "method": "initialize",
    "params": {}
  }'
```

And list tools:

```bash
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "id": "tools-1",
    "method": "tools/list",
    "params": {}
  }'
```

## What this MCP server does

- Exposes Coroot [incidents](https://docs.coroot.com/alerting/incidents/) and [application health summaries](https://github.com/coroot/coroot#application-health-summary) as MCP tools that an AI assistant can call.
- Provides natural-language root-cause summaries grounded in Coroot data via [AI-powered Root Cause Analysis](https://docs.coroot.com/ai/) concepts, implemented here with Spring AI.
- Returns compact JSON payloads for incidents and summaries so downstream tooling (postmortems, executive reports, dashboards, etc.) can build on top.

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

- Image: `ghcr.io/meritocra/coroot-mcp:0.1.0`

Run against a real Coroot instance:

```bash
docker run --rm -p 8080:8080 \
  -e OPENAI_API_KEY=sk-... \
  -e MCP_AUTH_TOKEN=some-secret-token \
  -e COROOT_API_URL=https://coroot.your-company.com \
  -e COROOT_DEFAULT_PROJECT_ID=production \
  ghcr.io/meritocra/coroot-mcp:0.1.0
```

Run in stub mode (no Coroot required):

```bash
docker run --rm -p 8080:8080 \
  -e MCP_AUTH_TOKEN=some-secret-token \
  -e SPRING_PROFILES_ACTIVE=stub-coroot \
  ghcr.io/meritocra/coroot-mcp:0.1.0
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

## Using with AI coding agents

This project speaks MCP over HTTP. Any MCP-aware coding assistant can talk to it once you point the client at the `/mcp` endpoint.

### Codex CLI

Assuming `coroot-mcp` runs on `http://localhost:8080/mcp`, add an MCP server entry in your Codex configuration, for example:

```toml
[mcp_servers.coroot-mcp]
transport = "http"
url = "http://localhost:8080/mcp"
```

Restart Codex CLI and list MCP servers to confirm that `coroot-mcp` is available.

### Claude / Claude Code

If you use a Claude-based environment that supports HTTP MCP servers, configure a new MCP server named `coroot-mcp` with:

- Transport: HTTP
- URL: `http://localhost:8080/mcp`

You can then call `list_recent_incidents` and `summarize_incident_root_cause` from within that environment.

## Available tools

The MCP server exposes tools via `tools/list` and `tools/call`. Currently implemented:

- `list_recent_incidents`
  - Returns a JSON array of recent incidents with id, title, service, severity, and timestamps.
  - Accepts optional filters: project ID, minimum severity, and result limit.
- `summarize_incident_root_cause`
  - Generates a natural-language explanation of an incident’s likely root cause, blast radius, and remediation steps.
  - Returns both human-readable text and a structured JSON representation of the incident context.

Both tools are read-only and safe to expose to assistants.

## Using with Toolhive (example)

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
