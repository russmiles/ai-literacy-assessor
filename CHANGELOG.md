# Changelog

---

## 2026-04-05

### Initial implementation — ALCI Assessor v0.1.0

- **Maven project and Spring Boot scaffold** — Kotlin 2.1.20, JDK 21, Embabel Agent Framework 0.1.0-SNAPSHOT, Spring Boot 3.4.5. Establishes the build foundation and multi-profile configuration (CLI vs web).

- **Domain model** — `AssessmentRequest`, `ObservableEvidence`, `ClarifyingQuestions/Responses`, `ALCIPlacement`, `ALCIDeepDive`, `LevelAssessment`, `Recommendations`, `TrajectoryComparison`, `AssessmentDocument`, `LiteracyLevel`. All the typed inputs and outputs the GOAP planner chains through.

- **Repository scanner** — `RepositoryScanner` reads a local Git repository and extracts observable AI literacy signals (CLAUDE.md, HARNESS.md, hook scripts, CI workflows, agent files) keyed to ALCI level evidence. Fully deterministic, no LLM.

- **ALCI instruments** — `PlacementInstrument` (Part A: 10 Likert statements per level, primary level computation) and `DeepDiveInstrument` (Part B practice-frequency/blocker/quantitative questions, Part C narrative questions). Both are data-driven, no LLM.

- **Interaction port** — `UserInteractionPort` interface abstracts all I/O so the agent is delivery-channel agnostic. `ShellInteraction` provides the terminal implementation (`@Profile("!web")`). `WebInteraction` provides the SSE/queue implementation (`@Profile("web")`).

- **ALCIAssessorAgent** — 10-action GOAP agent orchestrating evidence gathering, ALCI instrument administration, LLM-driven level assessment and recommendation generation, trajectory comparison, document assembly, and disk write. Uses three model roles (fast / reasoning / balanced) from `application.yml`.

- **Web chat interface** — `ChatController` with three endpoints (POST `/api/assess`, GET `/api/chat/stream` SSE, POST `/api/chat/respond`) backed by `WebInteraction`'s blocking queues. Vanilla JS single-page UI at `src/main/resources/static/index.html` with message-type prefix styling (QUESTION / SCALE / PROGRESS / RESULT).

- **Dockerfile** — Multi-stage build: Maven 3.9 + Eclipse Temurin 21 build stage, Alpine JRE runtime stage. Volume mounts `/repo` and `/assessments`. `EXPOSE 8080`.
