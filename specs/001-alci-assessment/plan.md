# Implementation Plan: ALCI Assessment Agent

References `specs/001-alci-assessment/spec.md` for all functional requirements
and acceptance scenarios.

---

## Module Structure

```text
com.ailiteracy.assessor/
  agent/
    ALCIAssessorAgent.kt      -- 10-action GOAP agent (FR-1 through FR-10)
  alci/
    PlacementInstrument.kt     -- Part A: 10 Likert statements, level computation (FR-4)
    DeepDiveInstrument.kt      -- Parts B+C: level-specific and narrative questions (FR-5)
  domain/
    AssessmentRequest.kt       -- Entry point: repo path + team name
    ObservableEvidence.kt      -- Scanner output: signal counts and level map
    ClarifyingQuestions.kt     -- LLM-generated questions from evidence gaps
    ClarifyingResponses.kt     -- User answers keyed by question text
    ALCIPlacement.kt           -- Part A result: scores, primary level, adjacent level
    ALCIDeepDive.kt            -- Parts B+C result: frequency, blockers, narratives
    LevelAssessment.kt         -- Judgment: level, discipline scores, rationale
    Recommendations.kt         -- 3-5 actionable items with framework references
    TrajectoryComparison.kt    -- Deltas and narrative vs previous assessment
    AssessmentDocument.kt      -- Assembled markdown + metadata
    LiteracyLevel.kt           -- L0-L5 enum
  interaction/
    UserInteractionPort.kt     -- I/O abstraction interface (FR-3, FR-4, FR-5)
    ShellInteraction.kt        -- Terminal implementation
    WebInteraction.kt          -- SSE/queue implementation
  scanner/
    RepositoryScanner.kt       -- Deterministic filesystem checks (FR-1)
  web/
    ChatController.kt          -- REST endpoints for web chat
```

## Data Model

The GOAP planner chains actions by matching types on the blackboard:

```text
AssessmentRequest
  -> ObservableEvidence (scanRepository)
    -> ClarifyingQuestions (generateQuestions)
      -> ClarifyingResponses (collectResponses)
        -> ALCIPlacement (administerPlacement)
          -> ALCIDeepDive (administerDeepDive)
            -> LevelAssessment (assessLevel, consumes ObservableEvidence + ALCIDeepDive)
              -> Recommendations (generateRecommendations)
              -> TrajectoryComparison (compareTrajectory)
                -> AssessmentDocument (assembleDocument, consumes all three)
                  -> String (writeAssessment, terminal goal)
```

## Algorithm Notes

### Repository Scanner (FR-1)

Three-pass scan mirroring the framework level taxonomy:

1. **L2 pass:** count `.github/workflows/*.yml`, grep for coverage/vuln/mutation
   keywords
2. **L3 pass:** check for CLAUDE.md, HARNESS.md, AGENTS.md, MODEL_ROUTING.md,
   REFLECTION_LOG.md; count skills, agents, commands; check for hooks
3. **L4 pass:** count `specs/*/spec.md` and `specs/*/plan.md`

All checks are file-existence or regex. No interpretation.

### Placement Instrument (FR-4)

- 10 statements: 2 per level (L0-L4), presented in shuffled order
- Each rated 1-5 on a Likert scale
- Primary level = level with highest mean score where mean >= 3.5
- Adjacent level = primary + 1 (growth edge), or primary - 1 if primary is L4

### Level Assessment (FR-6)

- Consumes both ObservableEvidence and ALCIDeepDive
- Uses reasoning model role (highest stakes)
- Primary level = highest level with substantial evidence across all three
  disciplines
- Weakest discipline is the ceiling

### Model Role Assignment

| Action | Model Role | Rationale |
| ------ | ---------- | --------- |
| scanRepository | none | Deterministic filesystem checks |
| generateQuestions | fast | Speed matters, structured extraction |
| collectResponses | none | Pure I/O collection |
| administerPlacement | none | Deterministic instrument |
| administerDeepDive | none | Deterministic instrument |
| assessLevel | reasoning | Highest-stakes judgment call |
| generateRecommendations | reasoning | Must be grounded in framework |
| compareTrajectory | fast | Mostly arithmetic with short narrative |
| assembleDocument | balanced | Good prose quality, not deep reasoning |
| writeAssessment | none | File I/O only |

## FR Mapping Table

| FR | Source File | Test File | Status |
| -- | ----------- | --------- | ------ |
| FR-1 | scanner/RepositoryScanner.kt | scanner/RepositoryScannerTest.kt | Implemented |
| FR-2 | agent/ALCIAssessorAgent.kt | agent/ALCIAssessorAgentTest.kt | Implemented |
| FR-3 | agent/ALCIAssessorAgent.kt | agent/ALCIAssessorAgentTest.kt | Implemented |
| FR-4 | alci/PlacementInstrument.kt | alci/PlacementInstrumentTest.kt | Implemented |
| FR-5 | agent/ALCIAssessorAgent.kt | agent/ALCIAssessorAgentTest.kt | Implemented |
| FR-6 | agent/ALCIAssessorAgent.kt | agent/ALCIAssessorAgentTest.kt | Implemented |
| FR-7 | agent/ALCIAssessorAgent.kt | agent/ALCIAssessorAgentTest.kt | Implemented |
| FR-8 | agent/ALCIAssessorAgent.kt | agent/ALCIAssessorAgentTest.kt | Implemented |
| FR-9 | agent/ALCIAssessorAgent.kt | agent/ALCIAssessorAgentTest.kt | Implemented |
| FR-10 | agent/ALCIAssessorAgent.kt | agent/ALCIAssessorAgentTest.kt | Implemented |

## Test Strategy

- **Unit tests for deterministic components:** RepositoryScanner uses `@TempDir`
  fixtures with directory structures mimicking real repos. PlacementInstrument
  tests verify level computation with known score distributions.
- **Agent action tests:** use FakeUserInteraction to stub I/O. Verify type
  contracts (input type -> output type) and side-effect boundaries.
- **No live LLM tests:** LLM-dependent actions are tested through their type
  contracts. Live integration testing is manual during development.
- **Coverage target:** 85% line coverage on `scanner` and `alci` packages
  (the deterministic, testable core).
