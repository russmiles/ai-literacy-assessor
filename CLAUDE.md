# ALCI Assessor -- Project Conventions

This is a Kotlin/Spring Boot application built with the Embabel Agent Framework.
It conducts AI Literacy Collaboration Index assessments through a conversational
interface (CLI or web chat). The domain model uses Kotlin data classes as typed
inputs and outputs for a GOAP agent planner.

## Build Commands

```bash
mvn test              # Run all JUnit 5 tests
mvn package           # Build the fat JAR (skips tests with -DskipTests)
docker build -t alci-assessor .   # Multi-stage Docker image
```

## Literate Programming

Every `.kt` file must open with a narrative preamble explaining why it exists,
key design decisions, and what it deliberately does not do. Read
`.claude/skills/literate-programming/SKILL.md` before creating or substantially
rewriting any source file.

The five rules in brief:

1. Every file opens with a narrative preamble
2. Documentation explains reasoning, not signatures
3. Order of presentation follows logical understanding
4. Each file has one clearly stated concern
5. Inline comments explain WHY, not WHAT

## CUPID Code Review

When reviewing or refactoring code, apply the CUPID lens documented at
`.claude/skills/cupid-code-review/SKILL.md`. Work through each property:
Composable, Unix philosophy, Predictable, Idiomatic, Domain-based.

## Conventional Comments

All review feedback uses Conventional Comments format:

```text
suggestion: consider extracting this into a helper
nitpick: trailing whitespace
issue: this silently swallows the exception
question: is this intentional? the spec says otherwise
```

## Embabel Agent Conventions

This project follows the Embabel Agent Framework patterns:

- **Domain objects are data classes** with meaningful field names from the
  assessment domain (not generic names like `input` or `result`)
- **Actions use typed I/O** -- each `@Action` function takes a domain object
  and returns a domain object. The GOAP planner chains actions by matching types.
- **DICE for LLM calls** -- Describe the role, provide Input context, specify
  Constraints on the output format, give Examples where helpful
- **Model roles** -- use `reasoning` for high-stakes judgment, `balanced` for
  prose assembly, `fast` for structured extraction. Never hard-code model names.
- **No side effects in actions** except the terminal `@AchievesGoal` action.
  Actions that need I/O go through `UserInteractionPort`.

## Spec-First Workflow

Any change to the assessment agent must flow through its spec before touching
implementation code:

1. Update the spec (`specs/001-alci-assessment/spec.md`)
2. Update the implementation plan (`specs/001-alci-assessment/plan.md`)
3. Write failing tests from the spec
4. Update the implementation
5. Refactor while keeping tests green

## Test-Driven Development

Follow red-green-refactor strictly:

1. Write a failing test that describes the desired behaviour
2. Write the minimal production code to make it pass
3. Clean up while keeping all tests green

Tests use JUnit 5, `@TempDir` for filesystem fixtures, and
`FakeUserInteraction` for agent tests that need I/O stubbing.

## Branch Discipline

Never commit directly to `main`. Create a descriptive branch before any changes.
After push, wait for CI checks to pass before declaring work done.

## Git Commit Messages

Write concise commit messages that describe what changed and why. No trailers,
no attribution lines, no postamble.
