# Specification: ALCI Assessment Agent

The ALCI Assessor conducts a complete AI Literacy Collaboration Index assessment
through a conversational interface. It scans repositories for observable
evidence, administers the ALCI instrument, assesses literacy level across three
disciplines, and generates recommendations.

---

## User Stories

### US-1: Team Lead Requests Assessment

As a team lead, I want to run an ALCI assessment against my repository so that
I understand my team's current AI literacy level and get actionable
recommendations for improvement.

**Acceptance scenarios:**

- **S1.1:** Given a repository path, the assessor scans it and reports observable
  evidence before asking any questions
- **S1.2:** Given no repository path, the assessor proceeds with conversational
  evidence gathering only
- **S1.3:** The assessor produces a markdown report file at the configured
  output directory

### US-2: Evidence-Based Assessment

As an assessor user, I want the assessment to combine objective evidence (file
scanning) with subjective evidence (my answers) so that the result reflects
both what exists in the repo and how the team actually works.

**Acceptance scenarios:**

- **S2.1:** The repository scan detects L2 signals (CI workflows, coverage,
  vulnerability scanning, mutation testing)
- **S2.2:** The repository scan detects L3 signals (CLAUDE.md, HARNESS.md,
  AGENTS.md, skills, agents, commands, hooks)
- **S2.3:** The repository scan detects L4 signals (specs with spec.md and
  plan.md)
- **S2.4:** Clarifying questions target gaps in the observable evidence
- **S2.5:** When the repo has strong L3 signals but no L4 signals, questions
  focus on spec-first practices

### US-3: ALCI Instrument Administration

As an assessor user, I want to complete the full ALCI instrument (Parts A, B,
and C) so that the assessment has standardised data to reason about.

**Acceptance scenarios:**

- **S3.1:** Part A presents 10 Likert-scale statements (2 per level, L0-L4) and
  computes a primary level
- **S3.2:** The adjacent level is one above the primary (growth edge), except
  when primary is L4 (adjacent is L3)
- **S3.3:** Part B administers level-specific questions for both primary and
  adjacent levels
- **S3.4:** Part B questions cover four categories: practice frequency, blocker/
  enabler, quantitative self-report, and open-ended
- **S3.5:** Part C administers three level-independent narrative questions

### US-4: Level Judgment

As an assessor user, I want the assessed level to be defensible -- grounded in
evidence, not just self-report -- so that I trust the result.

**Acceptance scenarios:**

- **S4.1:** The assessment produces a primary literacy level (L0-L5)
- **S4.2:** The assessment scores three disciplines: Context Engineering,
  Constraint Practice, Guardrail Deployment (each 0-5)
- **S4.3:** The primary level is the highest level with substantial evidence
  across all three disciplines
- **S4.4:** The assessment includes a rationale explaining why this level and not
  adjacent levels
- **S4.5:** The assessment identifies 2-4 strengths and 2-4 gaps

### US-5: Actionable Recommendations

As a team lead, I want 3-5 specific recommendations that I can act on this
quarter, with framework references, so that I know exactly what to do next.

**Acceptance scenarios:**

- **S5.1:** Each recommendation is a concrete action (not generic advice)
- **S5.2:** Each recommendation targets the weakest discipline or most impactful
  gap
- **S5.3:** Each recommendation includes a framework reference
- **S5.4:** Recommendations are ordered by impact

### US-6: Trajectory Tracking

As a team that has been assessed before, I want to see how our scores have
changed so that we can track progress over time.

**Acceptance scenarios:**

- **S6.1:** First assessment establishes a baseline with zero deltas
- **S6.2:** Subsequent assessments compare discipline scores with the previous
  assessment
- **S6.3:** The trajectory includes a narrative summary of progress

### US-7: Delivery Channel Agnostic

As a user, I want to run the assessment via CLI or web chat with identical
assessment quality, so that my choice of interface does not affect the result.

**Acceptance scenarios:**

- **S7.1:** All user I/O flows through UserInteractionPort
- **S7.2:** ShellInteraction works without the web profile
- **S7.3:** WebInteraction works under the web profile with SSE streaming
- **S7.4:** The agent code has no direct references to terminal or HTTP concepts

---

## Functional Requirements

| ID | Description | Actions | Scenarios |
| -- | ----------- | ------- | --------- |
| FR-1 | Scan repository for observable AI literacy signals | scanRepository | S1.1, S2.1-S2.3 |
| FR-2 | Generate clarifying questions from evidence gaps | generateQuestions | S2.4, S2.5 |
| FR-3 | Collect user responses to clarifying questions | collectResponses | S2.4 |
| FR-4 | Administer ALCI Part A placement instrument | administerPlacement | S3.1, S3.2 |
| FR-5 | Administer ALCI Parts B and C deep-dive | administerDeepDive | S3.3-S3.5 |
| FR-6 | Assess literacy level from all evidence | assessLevel | S4.1-S4.5 |
| FR-7 | Generate actionable recommendations | generateRecommendations | S5.1-S5.4 |
| FR-8 | Compare trajectory with previous assessment | compareTrajectory | S6.1-S6.3 |
| FR-9 | Assemble findings into markdown document | assembleDocument | S1.3 |
| FR-10 | Write assessment document to disk | writeAssessment | S1.3 |

---

## Non-Functional Requirements

- **NFR-1:** The four deterministic actions (FR-1, FR-3, FR-4, FR-5) must
  complete without any LLM API calls
- **NFR-2:** LLM-dependent actions must use the model role specified in
  application.yml, not hard-coded model names
- **NFR-3:** The assessment must complete within 15 minutes for a typical
  repository (under 1000 files)
- **NFR-4:** The output markdown must be valid, well-structured, and readable
  by a non-technical manager
