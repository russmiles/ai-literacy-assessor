# Changelog

---

## 2026-04-03

### Constraint promotion and habitat completion

- **JaCoCo coverage enforcement (C3)** -- Added `jacoco-maven-plugin` 0.8.12
  to pom.xml with `prepare-agent`, `report`, and `check` executions. Coverage
  floor set at 40% instruction coverage (current actual: ~45%). CI now runs
  `mvn verify` instead of `mvn test` so the coverage gate is active on every PR.

- **OWASP vulnerability scanning (C4)** -- Added `dependency-check-maven` step
  to `kotlin-tests.yml`. Advisory mode (`failBuildOnAnyVulnerability=false`)
  until Embabel SNAPSHOT transitive deps stabilise. Scan results visible in
  every PR run.

- **PIT mutation testing (C7)** -- Added `pitest-maven` 1.17.4 with
  `pitest-junit5-plugin` 1.2.1 to pom.xml. New weekly workflow
  `mutation-testing.yml` runs `pitest:mutationCoverage` and uploads the HTML
  report as a numbered artifact for score tracking over time.

- **Docker build verification (C8)** -- Added `docker build -t alci-assessor:test .`
  step to `kotlin-tests.yml`. Multi-stage build is now verified on every PR.

- **Enforcement ratio 8/8 (100%)** -- All four previously-unverified constraints
  are now wired into CI. Harness Status, README badge, and Enforcement Map
  updated to reflect the new ratio.

- **Operating cadence in CLAUDE.md** -- Quarterly cadence section added
  documenting the five recurring tasks (`/assess`, `/harness-audit`, reflection
  review, mutation trend check, `/harness-health` snapshot).

- **First health snapshot** -- `observability/snapshots/2026-04-03-snapshot.md`
  records the constraint state, CI workflow inventory, and coverage baseline
  after promotion.

- **First reflection** -- Entry added to REFLECTION_LOG.md and promoted to
  AGENTS.md with three new GOTCHAS: Embabel version coordination, JaCoCo
  threshold calibration, and OWASP false positives.

## 2026-04-05

### Initial implementation -- ALCI Assessor v0.1.0

- **Maven project and Spring Boot scaffold** -- Kotlin 2.1.20, JDK 21,
  Embabel Agent Framework 0.1.0-SNAPSHOT, Spring Boot 3.4.5. Establishes
  the build foundation and multi-profile configuration (CLI vs web).

- **Domain model** -- `AssessmentRequest`, `ObservableEvidence`,
  `ClarifyingQuestions/Responses`, `ALCIPlacement`, `ALCIDeepDive`,
  `LevelAssessment`, `Recommendations`, `TrajectoryComparison`,
  `AssessmentDocument`, `LiteracyLevel`. All the typed inputs and outputs
  the GOAP planner chains through.

- **Repository scanner** -- `RepositoryScanner` reads a local Git
  repository and extracts observable AI literacy signals (CLAUDE.md,
  HARNESS.md, hook scripts, CI workflows, agent files) keyed to ALCI
  level evidence. Fully deterministic, no LLM.

- **ALCI instruments** -- `PlacementInstrument` (Part A: 10 Likert
  statements per level, primary level computation) and
  `DeepDiveInstrument` (Part B practice-frequency/blocker/quantitative
  questions, Part C narrative questions). Both are data-driven, no LLM.

- **Interaction port** -- `UserInteractionPort` interface abstracts all
  I/O so the agent is delivery-channel agnostic. `ShellInteraction`
  provides the terminal implementation (`@Profile("!web")`).
  `WebInteraction` provides the SSE/queue implementation
  (`@Profile("web")`).

- **ALCIAssessorAgent** -- 10-action GOAP agent orchestrating evidence
  gathering, ALCI instrument administration, LLM-driven level assessment
  and recommendation generation, trajectory comparison, document assembly,
  and disk write. Uses three model roles (fast / reasoning / balanced)
  from `application.yml`.

- **Web chat interface** -- `ChatController` with three endpoints
  (POST `/api/assess`, GET `/api/chat/stream` SSE,
  POST `/api/chat/respond`) backed by `WebInteraction`'s blocking queues.
  Vanilla JS single-page UI at `src/main/resources/static/index.html`
  with message-type prefix styling.

- **Dockerfile** -- Multi-stage build: Maven 3.9 + Eclipse Temurin 21
  build stage, Alpine JRE runtime stage. Volume mounts `/repo` and
  `/assessments`. `EXPOSE 8080`.
