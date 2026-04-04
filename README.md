# ALCI Assessor

An AI Literacy assessment chatbot built with the [Embabel Agent Framework](https://github.com/embabel/embabel-agent).

Conducts the AI Literacy Collaboration Index (ALCI) assessment through
a conversational interface — CLI or web chat. Scans repositories for
observable evidence, administers the ALCI instrument, assesses literacy
level across three disciplines, and generates recommendations.

## Quick Start

```bash
# CLI mode (Anthropic only — one API key to get started)
docker run -it \
  -v /path/to/project:/repo \
  -v ./assessments:/assessments \
  -e ANTHROPIC_API_KEY=your-key \
  alci-assessor

# CLI mode (dual provider — cost optimised)
docker run -it \
  -v /path/to/project:/repo \
  -v ./assessments:/assessments \
  -e ANTHROPIC_API_KEY=your-anthropic-key \
  -e OPENAI_API_KEY=your-openai-key \
  -e SPRING_PROFILES_ACTIVE=openai \
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

| Variable | Required | Default | Description |
|---|---|---|---|
| `ANTHROPIC_API_KEY` | Yes | — | Anthropic API key for reasoning-tier LLM calls |
| `OPENAI_API_KEY` | No | — | OpenAI API key for balanced/fast-tier calls (cost optimisation) |
| `SPRING_PROFILES_ACTIVE` | No | (shell) | Set to `web` for chat UI, `openai` for dual-provider mode, or `web,openai` for both |
| `ASSESSOR_OUTPUT_DIR` | No | `./assessments` | Directory for assessment markdown output |

### LLM Provider Modes

**Anthropic only (default)** — one API key, all roles use Claude:

```bash
docker run -it -e ANTHROPIC_API_KEY=your-key alci-assessor
```

**Dual provider (cost optimised)** — Claude for reasoning, OpenAI for
balanced/fast phases. Roughly 40-60% cheaper per assessment:

```bash
docker run -it \
  -e ANTHROPIC_API_KEY=your-anthropic-key \
  -e OPENAI_API_KEY=your-openai-key \
  -e SPRING_PROFILES_ACTIVE=openai \
  alci-assessor
```

**Custom role overrides** — set model roles individually:

```bash
docker run -it \
  -e ANTHROPIC_API_KEY=your-key \
  -e EMBABEL_LLM_ROLE_BALANCED=openai \
  -e EMBABEL_LLM_ROLE_FAST=openai-mini \
  alci-assessor
```

### Model Routing

| Role | Anthropic Only | Dual Provider | Used By |
| ---- | -------------- | ------------- | ------- |
| `reasoning` | Claude Sonnet 4 | Claude Sonnet 4 | Level assessment, recommendations |
| `balanced` | Claude Sonnet 4 | GPT-4o | Document assembly, trajectory |
| `fast` | Claude Sonnet 4 | GPT-4o-mini | Question generation |

Of 10 agent actions, only 2 use the `reasoning` tier. 4 actions use
no LLM at all (scanning, user I/O, questionnaire scoring, file writing).

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
