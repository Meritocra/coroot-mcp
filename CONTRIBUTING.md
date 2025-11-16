# Contributing

Thank you for your interest in improving `coroot-mcp`.

This document describes how to build the project locally, run the tests, and propose changes.

## Getting started

### Prerequisites

- Java 21 or newer
- Maven 3.9 or newer
- Docker (optional, for container-based workflows)

Clone the repository:

```bash
git clone https://github.com/Meritocra/coroot-mcp.git
cd coroot-mcp
```

### Build and test

Run the full test suite:

```bash
./mvnw test
```

This will execute unit and integration tests and generate JaCoCo coverage reports under `target/site/jacoco`.

To run the application locally with the stub Coroot client:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=stub-coroot
```

The MCP endpoint will be available at `http://localhost:8080/mcp`.

To run the application locally against a real Coroot instance:

```bash
export OPENAI_API_KEY=sk-...
export COROOT_API_URL=https://coroot.your-company.com
export COROOT_DEFAULT_PROJECT_ID=production

./mvnw spring-boot:run
```

## Coding guidelines

- Keep changes focused and incremental.
- Follow the existing code style and structure.
- Ensure tests pass (`./mvnw test`) before opening a pull request.
- Add or update tests when you change behavior or add new features.

## Proposing changes

1. Open an issue describing the bug or feature you want to work on, or comment on an existing issue to indicate interest.
2. Fork the repository and create a branch for your work.
3. Make your changes and add tests as needed.
4. Run `./mvnw test` to confirm everything still passes.
5. Open a pull request with a clear description of:
   - The problem being addressed
   - How you solved it
   - Any trade-offs or follow-up work you see

Please keep pull requests focused; if you want to tackle multiple unrelated changes, split them into separate PRs.

## Reporting issues

When filing an issue, include:

- A short description of the problem
- Steps to reproduce
- Expected behavior
- Actual behavior
- Environment details (OS, Java version, how you ran the server)

This helps maintainers understand and address the problem more quickly.

