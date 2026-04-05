# AGENTS.md -- Compound Learning Memory

Accumulated conventions and decisions that AI agents working on this codebase
should know. Entries are grouped by concern and updated as the team learns.

---

## STYLE

- **Kotlin stdlib preferred** -- use `buildString`, `mapOf`, `listOf`,
  `forEachIndexed`, and other stdlib idioms rather than Java collection patterns.
  Kotlin's standard library is expressive enough that Java interop should only
  appear when calling Spring or Embabel APIs.

- **Literate preambles on every .kt file** -- every Kotlin source file opens
  with a block comment explaining why the file exists, what design decisions it
  embodies, and what it deliberately does not do. This is not optional and
  applies to test files too.

- **CUPID review lens** -- code review applies the five CUPID properties
  (Composable, Unix, Predictable, Idiomatic, Domain-based) rather than SOLID.
  See `.claude/skills/cupid-code-review/SKILL.md`.

- **Conventional Comments** -- all review feedback follows the Conventional
  Comments format (`suggestion:`, `issue:`, `nitpick:`, `question:`).

## ARCH_DECISIONS

- **Embabel GOAP for sequential assessment flow** -- the assessment is modelled
  as a 10-action goal-oriented agent where the GOAP planner discovers the action
  chain from type signatures. This was chosen over a hard-coded pipeline because
  it allows the planner to re-route when an action fails (e.g., skip trajectory
  comparison if no previous assessment exists). The trade-off is that action
  ordering is implicit in the type chain rather than explicit in a sequence.

- **UserInteractionPort abstracts CLI/web** -- all user-facing I/O flows through
  the `UserInteractionPort` interface. `ShellInteraction` handles terminal I/O
  (active without the `web` profile), `WebInteraction` handles SSE-backed web
  chat (active under the `web` profile). The agent never knows which channel it
  is talking to. This was chosen because the assessment conversation is identical
  regardless of delivery channel -- only the I/O mechanism differs.

- **Model roles per action, not per agent** -- each `@Action` declares its own
  model role (`reasoning`, `balanced`, `fast`) rather than the agent having a
  single model. This allows cost optimisation: only 2 of 10 actions use the
  expensive reasoning tier. 4 actions use no LLM at all.

- **Domain objects as GOAP blackboard entries** -- every intermediate result
  (ObservableEvidence, ClarifyingQuestions, ALCIPlacement, etc.) is a Kotlin
  data class placed on the GOAP blackboard. The planner matches these types to
  discover action chains. This means adding a new assessment phase is just
  adding a new data class and a new `@Action` that consumes/produces it.

## GOTCHAS

- **embabel-version-coordination** -- The Embabel Agent Framework publishes
  SNAPSHOT versions to `repo.embabel.com/artifactory`, not Maven Central. The
  0.3.0 release is aspirational. Always verify actual available coordinates
  before configuring pom.xml. The custom repository block is required in every
  project that depends on Embabel SNAPSHOTs.

- **jacoco-threshold-calibration** -- When adding JaCoCo, always run
  `mvn verify` locally first and read the actual covered-ratio from the warning
  message before setting the `<minimum>` threshold. Start at (actual − 0.05)
  and raise incrementally. Setting the threshold above actual coverage blocks
  the PR immediately.

- **owasp-snapshot-false-positives** -- OWASP dependency-check may flag
  Embabel SNAPSHOT transitive deps as vulnerabilities (unresolved CVEs in
  snapshot builds). Use `failBuildOnAnyVulnerability=false` until the
  dependency tree stabilises on release versions.

## TEST_STRATEGY

- **TDD with JUnit 5** -- all production code is written test-first. The test
  suite currently covers the scanner, placement instrument, and agent action
  signatures.

- **@TempDir for filesystem fixtures** -- `RepositoryScannerTest` creates
  temporary directory structures that mimic real repositories, so tests run
  without touching the actual filesystem. Each test method gets a fresh temp
  directory.

- **FakeUserInteraction for agent tests** -- agent tests that exercise actions
  requiring user I/O use a `FakeUserInteraction` implementation that returns
  pre-programmed responses. This keeps agent tests deterministic and fast.

- **No integration tests against live LLMs** -- all LLM-dependent actions are
  tested through their type signatures (input/output contracts) rather than
  through live API calls. Live integration testing is done manually during
  development.
