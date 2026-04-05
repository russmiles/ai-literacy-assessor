# Skill: CUPID Code Review

Apply Daniel Terhorst-North's CUPID properties as a structured review lens.
CUPID replaces SOLID for this project because it focuses on properties that
make code joyful to work with rather than abstract design principles.

---

## The Five Properties

### 1. Composable

Can this code be used independently, without hidden dependencies or setup
rituals?

**Review questions:**

- Can I call this function/class without knowing about its siblings?
- Does it require specific global state, environment variables, or init order?
- Can I extract it into another project and have it work?
- Are its dependencies explicit (constructor injection, parameters) rather than
  ambient (static singletons, service locators)?

**Refactoring signals:**

- Function requires calling `init()` first
- Class reads from a global config object directly
- Test setup requires standing up unrelated infrastructure
- Method signature hides a dependency on shared mutable state

**In this project:** Embabel actions should be composable by design -- each
takes a typed input and returns a typed output. The GOAP planner handles
composition. Watch for actions that reach outside their type contract (e.g.,
reading from the filesystem directly instead of through the scanner).

### 2. Unix Philosophy

Does this code do one thing completely and well?

**Review questions:**

- Can I describe what this does in one sentence without using "and"?
- Does it handle the full lifecycle of its one concern (happy path, errors,
  edge cases)?
- Is there functionality here that belongs in a different module?
- Would splitting this make both halves more useful?

**Refactoring signals:**

- Class name contains "And" or "Manager" or "Utils"
- Method has multiple unrelated responsibilities separated by blank lines
- File is longer than 200 lines (in Kotlin, this usually means too many concerns)
- Function has more than two levels of nesting

**In this project:** Each `@Action` should do exactly one assessment phase.
The agent file (`ALCIAssessorAgent.kt`) is the one place where all actions
live, but each action method should be independently understandable. Domain
classes should each represent one concept in the assessment vocabulary.

### 3. Predictable

Does this code behave as its name suggests, with no hidden side effects?

**Review questions:**

- Does the function name accurately describe ALL of its effects?
- Are there hidden writes (logging, metrics, file I/O) not implied by the name?
- Does it modify its inputs or only its return value?
- Would a caller be surprised by anything this function does?

**Refactoring signals:**

- Function named `getX()` that also modifies state
- Method that logs errors but does not propagate them
- Action that writes to disk as a side effect (only the terminal action should)
- Return type is `Unit` but the function has observable effects not in its name

**In this project:** Only `writeAssessment` (the `@AchievesGoal` action) should
have filesystem side effects. All other actions should be pure transformations
from input type to output type, with user I/O flowing through
`UserInteractionPort` (which is an explicit, named dependency).

### 4. Idiomatic

Does this code follow the grain of the language and project conventions?

**Review questions:**

- Does it use Kotlin idioms (`data class`, `when`, `let`, `apply`, extension
  functions) rather than Java patterns?
- Does it follow the project's established patterns (literate preambles, domain
  data classes, typed action I/O)?
- Would a Kotlin developer feel at home reading this?
- Does it use the standard library rather than reinventing utilities?

**Refactoring signals:**

- Java-style `if (x != null)` instead of Kotlin's `?.let { }` or safe calls
- Mutable collections where `buildList` or `mapOf` would work
- Missing `data` modifier on a class that is just a value holder
- String concatenation instead of `buildString` or string templates

**In this project:** The codebase uses Kotlin 2.1 with Spring Boot. Embabel
annotations (`@Agent`, `@Action`, `@AchievesGoal`) are the framework idiom.
Spring conventions (constructor injection, `@Component`, `@Value`, profiles)
are the infrastructure idiom. Both should be followed consistently.

### 5. Domain-based

Do the names in this code come from the problem domain, not technical
implementation?

**Review questions:**

- Would a non-programmer familiar with AI literacy assessment understand the
  class and method names?
- Do variable names describe business concepts (assessment, placement, evidence)
  rather than technical roles (handler, processor, manager)?
- Does the package structure mirror the domain (agent, domain, scanner, alci)
  rather than technical layers (controller, service, repository)?

**Refactoring signals:**

- Class named `AssessmentService` instead of something domain-specific
- Variable named `result` or `data` instead of `placement` or `evidence`
- Package named `util` or `helper` containing domain logic
- Method named `process()` instead of `assessLevel()` or `scanRepository()`

**In this project:** The domain vocabulary comes from the AI Literacy
Collaboration Index: levels (L0-L5), disciplines (context engineering,
constraint practice, guardrail deployment), instruments (placement, deep-dive),
and evidence signals. Code should use these terms directly.

---

## Review Workflow

When reviewing a file or PR:

1. Read the file's literate preamble first
2. For each CUPID property, ask the review questions above
3. Record findings using Conventional Comments format
4. Classify each finding: `suggestion:`, `issue:`, `nitpick:`, `question:`
5. Return PASS if no `issue:` findings, otherwise return the findings list

A file can PASS with `suggestion:` and `nitpick:` findings. Only `issue:`
findings block approval.
