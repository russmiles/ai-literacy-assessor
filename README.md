# ALCI Assessor

![Kotlin Tests](https://github.com/russmiles/ai-literacy-assessor/actions/workflows/kotlin-tests.yml/badge.svg)
![Lint Markdown](https://github.com/russmiles/ai-literacy-assessor/actions/workflows/lint-markdown.yml/badge.svg)
![Harness](https://img.shields.io/badge/harness-8%2F8%20enforced-brightgreen)
![AI Literacy](https://img.shields.io/badge/AI%20Literacy-L4%20Scaling-blue)

An AI Literacy assessment chatbot built with the [Embabel Agent Framework](https://github.com/embabel/embabel-agent).

Conducts the AI Literacy Collaboration Index (ALCI) assessment through
a conversational interface -- CLI or web chat. Scans repositories for
observable evidence, administers the ALCI instrument, assesses literacy
level across three disciplines, and generates recommendations.

## Quick Start

```bash
# CLI mode (Anthropic only -- one API key to get started)
docker run -it \
  -v /path/to/project:/repo \
  -v ./assessments:/assessments \
  -e ANTHROPIC_API_KEY=your-key \
  alci-assessor

# CLI mode (dual provider -- cost optimised)
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
# Compile, test, and check coverage
mvn verify

# Build the fat JAR
mvn package -DskipTests

# Build the Docker image
docker build -t alci-assessor .
```

## Configuration

| Variable | Required | Default | Description |
| -------- | -------- | ------- | ----------- |
| `ANTHROPIC_API_KEY` | Yes | -- | Anthropic API key for reasoning-tier LLM calls |
| `OPENAI_API_KEY` | No | -- | OpenAI API key for balanced/fast calls |
| `SPRING_PROFILES_ACTIVE` | No | (shell) | `web`, `openai`, or `web,openai` |
| `ASSESSOR_OUTPUT_DIR` | No | `./assessments` | Directory for assessment output |

### LLM Provider Modes

**Anthropic only (default)** -- one API key, all roles use Claude:

```bash
docker run -it -e ANTHROPIC_API_KEY=your-key alci-assessor
```

**Dual provider (cost optimised)** -- Claude for reasoning, OpenAI for
balanced/fast phases. Roughly 40-60% cheaper per assessment:

```bash
docker run -it \
  -e ANTHROPIC_API_KEY=your-anthropic-key \
  -e OPENAI_API_KEY=your-openai-key \
  -e SPRING_PROFILES_ACTIVE=openai \
  alci-assessor
```

**Custom role overrides** -- set model roles individually:

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

- `ShellInteraction` -- active without the `web` profile; reads/writes the terminal
- `WebInteraction` -- active under the `web` profile; bridges the blocking agent
  thread to the browser via `LinkedBlockingQueue` pairs and SSE

Three phases:

1. **Evidence** -- repository scan + clarifying questions + ALCI Part A placement + Part B/C deep dive
2. **Judgment** -- LLM level assessment + recommendations + trajectory comparison
3. **Delivery** -- document assembly + markdown write to `/assessments/`

## Enforcement Mechanism Map

```text
ENFORCEMENT LOOPS
=================

PR Loop (every push/PR)
  deterministic:
    lint-markdown.yml -----> markdownlint-cli2 "**/*.md"
    kotlin-tests.yml ------> mvn -B verify (tests + JaCoCo coverage check)
    kotlin-tests.yml ------> OWASP dependency-check (advisory)
    kotlin-tests.yml ------> docker build -t alci-assessor:test .
  agent:
    code-reviewer ---------> CUPID + LP review (Conventional Comments)

Weekly Loop (Dependabot + GC rules + mutation testing)
  deterministic:
    dependabot.yml --------> maven + github-actions updates
    mutation-testing.yml --> mvn pitest:mutationCoverage (score tracking)
  agent:
    GC1 doc-freshness -----> README, CHANGELOG, HARNESS staleness check
    GC2 convention-drift --> CLAUDE.md vs actual code patterns
    GC3 stale-agents ------> AGENTS.md entry age check
    GC4 snapshot-staleness -> observability/snapshots age check

Quarterly Loop (manual)
  assessment ------------> /assess (ALCI self-assessment)
  harness-audit ---------> constraint drift check
  reflection-review -----> REFLECTION_LOG.md promotion
  mutation-trends -------> compare quarterly PIT scores
  cost-review -----------> MODEL_ROUTING.md vs actual spend
```

## Harness Status

| Metric | Count |
| ------ | ----- |
| Constraints | 8 |
| Enforced (deterministic + agent) | 8 |
| Unverified | 0 |
| GC rules | 5 |

See [HARNESS.md](HARNESS.md) for the full constraint catalogue.

## Project Structure

| Path | Purpose |
| ---- | ------- |
| `CLAUDE.md` | Project conventions for AI coding tools |
| `AGENTS.md` | Compound learning memory |
| `HARNESS.md` | Enforcement constraint catalogue |
| `MODEL_ROUTING.md` | Model tier assignments for development agents |
| `REFLECTION_LOG.md` | Post-task reflections |
| `.claude/skills/` | 2 skills (literate-programming, cupid-code-review) |
| `.claude/agents/` | 4 agents (orchestrator, implementer, reviewer, integration) |
| `.claude/commands/` | 1 command (/reflect) |
| `specs/001-alci-assessment/` | Spec and implementation plan |
| `observability/snapshots/` | Health snapshot directory |

## License

[Apache License 2.0](LICENSE)
