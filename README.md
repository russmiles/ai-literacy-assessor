# ALCI Assessor

An AI Literacy assessment chatbot built with the [Embabel Agent Framework](https://github.com/embabel/embabel-agent).

Conducts the AI Literacy Collaboration Index (ALCI) assessment through
a conversational interface — CLI or web chat. Scans repositories for
observable evidence, administers the ALCI instrument, assesses literacy
level across three disciplines, and generates recommendations.

## Quick Start

```bash
# CLI mode
docker run -it \
  -v /path/to/project:/repo \
  -v ./assessments:/assessments \
  -e ANTHROPIC_API_KEY=your-key \
  alci-assessor

# Web mode
docker run -p 8080:8080 \
  -v /path/to/project:/repo \
  -v ./assessments:/assessments \
  -e ANTHROPIC_API_KEY=your-key \
  -e SPRING_PROFILES_ACTIVE=web \
  alci-assessor
```

Open `http://localhost:8080` after the container starts. The chat UI loads
automatically and begins the assessment session.

## Build

```bash
# Compile and test
mvn test

# Build the fat JAR
mvn package -DskipTests

# Build the Docker image
docker build -t alci-assessor .
```

## Configuration

| Variable | Default | Description |
|---|---|---|
| `ANTHROPIC_API_KEY` | required | Anthropic API key for LLM calls |
| `SPRING_PROFILES_ACTIVE` | (shell) | Set to `web` to enable the chat UI |
| `ASSESSOR_OUTPUT_DIR` | `./assessments` | Directory for assessment markdown output |

Model role assignments (fast / balanced / reasoning) are configured in
`src/main/resources/application.yml`.

## Architecture

The assessment runs as a 10-action GOAP agent (`ALCIAssessorAgent`). All user
I/O flows through `UserInteractionPort`, which has two implementations:

- `ShellInteraction` — active without the `web` profile; reads/writes the terminal
- `WebInteraction` — active under the `web` profile; bridges the blocking agent
  thread to the browser via `LinkedBlockingQueue` pairs and SSE

Three phases:

1. **Evidence** — repository scan + clarifying questions + ALCI Part A placement + Part B/C deep dive
2. **Judgment** — LLM level assessment + recommendations + trajectory comparison
3. **Delivery** — document assembly + markdown write to `/assessments/`

## License

[Apache License 2.0](LICENSE)
