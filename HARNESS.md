# Harness -- ALCI Assessor

The enforcement surface for the ALCI Assessor project. Documents every
constraint, its enforcement mechanism, and the current state of compliance.

---

## Context

| Dimension | Value |
| --------- | ----- |
| Language | Kotlin 2.1.20 |
| Build | Maven 3.9+ |
| Framework | Embabel Agent Framework 0.1.0-SNAPSHOT |
| Runtime | Spring Boot 3.4.5, JDK 21 |
| Test | JUnit 5, Spring Boot Test |
| Container | Docker (multi-stage, Eclipse Temurin 21 Alpine) |
| Shell | Spring Shell (CLI mode) |

## Conventions

- **Domain modelling** -- Kotlin data classes for all typed inputs/outputs.
  Field names come from the assessment domain, not generic programming terms.
- **Literate preambles** -- every `.kt` file opens with a narrative block
  comment explaining why it exists and what it does not do.
- **CUPID review** -- code review applies the five CUPID properties rather
  than SOLID.
- **Conventional Comments** -- all review feedback uses the format
  `suggestion:`, `issue:`, `nitpick:`, `question:`.
- **Spec-first changes** -- any feature change flows through the spec before
  touching implementation code.

## Constraints

### C1: Markdown formatting

All markdown files pass `markdownlint-cli2` with no errors.

- **Enforcement**: deterministic
- **Tool**: `npx markdownlint-cli2 "**/*.md"`
- **Scope**: pr
- **CI**: `.github/workflows/lint-markdown.yml`

### C2: Kotlin tests pass

All JUnit 5 tests pass on every push and pull request.

- **Enforcement**: deterministic
- **Tool**: `mvn -B test`
- **Scope**: pr
- **CI**: `.github/workflows/kotlin-tests.yml`

### C3: Kotlin test coverage

JaCoCo coverage on `scanner` and `alci` packages meets 85% line threshold.

- **Enforcement**: unverified
- **Tool**: JaCoCo Maven plugin (not yet configured in pom.xml)
- **Scope**: pr
- **CI**: not yet wired

### C4: Vulnerability scanning

Dependencies are checked for known CVEs on every PR.

- **Enforcement**: unverified
- **Tool**: OWASP dependency-check-maven or similar
- **Scope**: pr
- **CI**: not yet wired

### C5: Literate programming compliance

Every `.kt` file has a narrative preamble following the five LP rules.

- **Enforcement**: agent
- **Tool**: harness-enforcer agent (reads `.claude/skills/literate-programming/SKILL.md`)
- **Scope**: pr

### C6: CUPID properties

Code review applies all five CUPID properties with no unresolved `issue:` findings.

- **Enforcement**: agent
- **Tool**: harness-enforcer agent (reads `.claude/skills/cupid-code-review/SKILL.md`)
- **Scope**: pr

### C7: Mutation testing score tracked

PIT mutation testing runs weekly and scores are tracked over time.

- **Enforcement**: unverified
- **Tool**: pitest-maven plugin (not yet configured)
- **Scope**: weekly
- **CI**: not yet wired

### C8: Docker image builds

The multi-stage Docker build completes without errors.

- **Enforcement**: unverified
- **Tool**: `docker build -t alci-assessor .`
- **Scope**: pr
- **CI**: not yet wired

## GC Rules

### GC1: Documentation freshness

README.md, CHANGELOG.md, and HARNESS.md are reviewed weekly for staleness.

- **Enforcement**: agent
- **Frequency**: weekly

### GC2: Convention drift

CLAUDE.md conventions are checked against actual code patterns weekly.

- **Enforcement**: agent
- **Frequency**: weekly

### GC3: Stale AGENTS.md

AGENTS.md entries older than two quarters are archived or promoted.

- **Enforcement**: agent
- **Frequency**: weekly

### GC4: Snapshot staleness

Observability snapshots older than 30 days trigger a re-scan.

- **Enforcement**: deterministic
- **Frequency**: weekly

### GC5: Dependency currency

Maven dependencies are checked for available updates weekly via Dependabot.

- **Enforcement**: deterministic
- **Frequency**: weekly
- **Tool**: `.github/dependabot.yml`

## Status

| Metric | Count |
| ------ | ----- |
| Total constraints | 8 |
| Deterministic, enforced | 2 |
| Agent, enforced | 2 |
| Unverified | 4 |
| Enforcement ratio | 4/8 (50%) |
| GC rules | 5 |
| GC deterministic | 2 |
| GC agent | 3 |
