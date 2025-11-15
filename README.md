# coroot-mcp

Coroot MCP is a Model Context Protocol (MCP) server that turns your Coroot observability stack into a set of well-typed tools an LLM assistant can call for root-cause analysis.

It is implemented as a Spring Boot 3 / Spring AI 1.1.x application and exposes a JSON-RPC 2.0 MCP endpoint over HTTP at `/mcp`.

The project is licensed under the MIT License.

## What this MCP server does

- Exposes Coroot incidents and service health as MCP tools that an AI assistant can call.
- Provides natural-language root-cause summaries grounded in Coroot data via Spring AI.
- Returns compact JSON payloads for incidents and summaries so downstream tooling (postmortems, exec reports, etc.) can build on top.

## Architecture and principles

- **12-factor friendly**: all secrets and environment-specific configuration are supplied via environment variables (e.g. `OPENAI_API_KEY`, `COROOT_API_URL`, `COROOT_DEFAULT_PROJECT_ID`).
- **SOLID/KISS/DRY**:
  - Clear separation between HTTP/MCP layer, tool abstraction, and Coroot domain client.
  - Read-only, side-effect-free MCP tools with explicit JSON schemas.
  - Minimal domain model for incidents and service health, reused across tools.
- **Stateless service**: the MCP server is stateless; Coroot and the LLM are treated as external backing services.

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

These map to Spring Boot configuration in `src/main/resources/application.properties`.

## Running locally (dev)

```bash
export OPENAI_API_KEY=sk-...
export COROOT_API_URL=https://coroot.your-company.com
export COROOT_DEFAULT_PROJECT_ID=production

./mvnw spring-boot:run
```

The MCP JSON-RPC endpoint will be available at:

- `POST http://localhost:8080/mcp`

## Docker / container image

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

## MCP manifest

The MCP manifest is defined in `mcp.json`. It declares:

- Server name and description.
- HTTP transport pointing to `http://localhost:8080/mcp`.
- Declared secrets: `OPENAI_API_KEY`, `COROOT_API_KEY`.

Point your MCP-compatible client (e.g. Toolhive / Toolhouse) at `mcp.json` to register this server.

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

## Testing and quality

- Uses JUnit + Spring Boot Test for unit and integration tests.
- JaCoCo is configured to generate coverage reports; target coverage is 60%+ across unit and integration tests.
- Code is structured to keep tests fast and deterministic (no real calls to OpenAI or Coroot in test scope).

To run tests and generate a coverage report:

```bash
./mvnw test
```

JaCoCo HTML reports will be generated under `target/site/jacoco`.
