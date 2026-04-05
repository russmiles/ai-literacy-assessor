# Model Routing -- Development Agent Team

How AI coding tools working on this project should select models. This is about
the development agents (orchestrator, code reviewer, implementer), not the
assessment LLM routing (which lives in `application.yml`).

---

## Tier Definitions

| Tier | Models | Cost | Use When |
| ---- | ------ | ---- | -------- |
| Reasoning | Claude Sonnet 4, Opus 4 | High | Architecture decisions, spec writing, complex debugging, code review |
| Balanced | Claude Sonnet 4, GPT-4o | Medium | Implementation, test writing, refactoring, document assembly |
| Fast | Claude Haiku, GPT-4o-mini | Low | Formatting, linting, simple lookups, commit message drafting |

## Agent-to-Tier Mapping

| Agent | Tier | Rationale |
| ----- | ---- | --------- |
| Orchestrator | Reasoning | Needs to understand the full project context and make sequencing decisions |
| Code Reviewer | Reasoning | Must apply CUPID + LP lenses with nuance; false negatives are expensive |
| Kotlin Implementer | Balanced | Implementation is guided by specs and failing tests; speed matters more than deep reasoning |
| Integration Agent | Fast | Mechanical tasks: changelog updates, commit formatting, PR creation |
| Spec Writer | Reasoning | Specifications require careful domain understanding and precise acceptance criteria |
| TDD Agent | Balanced | Test writing follows established patterns; the spec provides the reasoning |

## Cost Guidelines

- Default to the **balanced** tier unless the task requires multi-file reasoning
  or subjective judgment
- Use **fast** for any task that is primarily string manipulation or follows a
  known template
- Use **reasoning** only when the output quality directly affects project
  correctness (specs, reviews, architecture)
- Never use reasoning tier for formatting, linting, or mechanical transforms
