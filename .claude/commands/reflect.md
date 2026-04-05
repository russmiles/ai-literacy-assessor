# /reflect -- Capture Post-Task Reflections

After completing a task, capture what was learned and add it to
REFLECTION_LOG.md.

---

## Instructions

1. Ask the user (or infer from the session) three things:
   - **What changed** -- a one-sentence summary of the task
   - **What was surprising** -- unexpected findings, edge cases, or learnings
   - **What future agents should know** -- conventions established, gotchas

1. Create a new entry in `REFLECTION_LOG.md` using this format:

```markdown
## YYYY-MM-DD -- [short title]

**What changed:** [description]

**What was surprising:** [findings]

**What future agents should know:** [conventions, gotchas]

**Promoted to AGENTS.md:** no
```

1. Check whether the reflection contains something that should be promoted to
   `AGENTS.md`:
   - A new architectural decision? Add to ARCH_DECISIONS
   - A new style convention? Add to STYLE
   - A new testing pattern? Add to TEST_STRATEGY

1. If promoted, update the `Promoted to AGENTS.md:` field to `yes` and add
   the entry to the appropriate section in AGENTS.md.
