# Skill: Literate Programming

Apply Don Knuth's literate programming principles to every source file. The goal
is code that reads as a narrative -- a human reader should understand the design
intent, constraints, and trade-offs without reading the implementation first.

---

## The Five Rules

### 1. Every file opens with a narrative preamble

The first thing in every `.kt` file is a block comment (`/** ... */`) that
explains:

- **Why this file exists** -- the problem it solves, stated in one sentence
- **Key design decisions** -- why this approach rather than alternatives
- **What it deliberately does NOT do** -- boundaries that prevent scope creep

The preamble is not a Javadoc summary of public API. It is a design narrative
for the next person who opens this file.

**Good example (Kotlin):**

```kotlin
/**
 * RepositoryScanner performs deterministic filesystem checks to detect AI
 * literacy signals in a local Git repository. Every check is a file-existence
 * or regex match -- no LLM is involved.
 *
 * The scanner deliberately does not interpret what it finds. It counts CI
 * workflows but does not decide whether four workflows is good or bad.
 * Interpretation belongs to the assessment agent's LLM-driven actions.
 *
 * When no repo path is provided, the scanner returns empty evidence with all
 * defaults. The assessment agent handles the fallback to conversational
 * evidence gathering.
 */
```

**Red flags:**

- File starts with `package` statement and no comment
- Preamble just restates the class name: "This is the RepositoryScanner class"
- Preamble lists method signatures instead of design intent

### 2. Documentation explains reasoning, not signatures

Comments and doc blocks explain WHY the design is this way, not WHAT the
function returns (the type signature already says that).

**Good:** "Uses the 'fast' model role because question generation needs speed,
not deep reasoning."

**Bad:** "Returns a ClarifyingQuestions object containing the generated
questions."

### 3. Order of presentation follows logical understanding

Within a file, code is ordered for a reader who is encountering the system for
the first time:

- Orchestration before detail
- Public API before private helpers
- Concept before mechanism
- Phase 1 before Phase 2 before Phase 3

In the ALCI agent, this means evidence-gathering actions appear before judgment
actions, which appear before delivery actions.

### 4. Each file has one clearly stated concern

The first sentence of the preamble names the single concern this file owns. If
you cannot state the concern in one sentence, the file does too much.

**Good:** "PlacementInstrument administers the ALCI Part A placement survey and
computes the primary literacy level from collected scores."

**Bad:** "This file handles placement, deep-dive questions, and scoring."

### 5. Inline comments explain WHY, not WHAT

The code itself shows what happens. Comments explain the reasoning behind
non-obvious decisions.

**Good:** `// Adjacent level is one below if primary is L4, because L5 is not
reachable through placement alone`

**Bad:** `// Set adjacent level`

---

## Rationalization Table

| Rule | What it prevents | What it enables |
| ---- | ---------------- | --------------- |
| Narrative preamble | "What does this file do?" questions | New contributors orient in seconds |
| Reasoning docs | Stale parameter-list Javadoc | Design intent survives refactoring |
| Logical order | Scrolling to find the entry point | Top-down reading works |
| One concern | God files that nobody wants to touch | Confident, scoped changes |
| WHY comments | Comment rot from restating the obvious | Comments that age well |

## Applying to This Project

This project is a Kotlin/Embabel agent application. Specific guidance:

- **Data classes** (`domain/` package) get preambles too -- explain why these
  fields exist and how the type participates in the GOAP type chain
- **@Action methods** should have doc comments explaining the model role choice
  and why this action is separate from adjacent actions in the chain
- **Test files** get preambles explaining the testing strategy and what fixtures
  are used
- **Configuration files** (application.yml, pom.xml) do not need preambles, but
  non-obvious settings should have inline comments
