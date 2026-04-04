/**
 * LevelAssessment is the core judgment output of the ALCI assessor. It synthesises
 * observable evidence, placement scores, and deep-dive responses into a single
 * authoritative statement of where the team stands.
 *
 * The three discipline scores — context engineering, constraint practice, and guardrail
 * deployment — reflect the ALCI framework's view that AI literacy is not a single
 * dimension. A team may be sophisticated at writing system prompts (context engineering)
 * while having no guardrails in production, or vice versa. The primary level is the
 * highest level where the team has substantial evidence across all three disciplines —
 * the weakest discipline is the ceiling, as the framework specifies.
 *
 * The [rationale] field is prose written by the reasoning model. It explains the
 * judgment, not just the conclusion — why this level and not the one above or below,
 * what the deciding evidence was, and what would need to change for the team to advance.
 * This reasoning is preserved in the final assessment document so the team can engage
 * with it, not just accept the verdict.
 *
 * This type does not contain recommendations. Recommendations are generated separately
 * in [Recommendations], because the logic for "where you are" and "what to do next"
 * are independent concerns — and because the recommendation agent can read this
 * assessment as input without coupling to how it was produced.
 */
package com.ailiteracy.assessor.domain

/**
 * The authoritative assessment of a team's current AI literacy level.
 *
 * Discipline scores use the range 0–5, matching the framework's level scale.
 * A score of 0 means no evidence of the practice; 5 means fully embedded and habitual.
 * The three scores are assessed independently — they will frequently differ.
 */
data class LevelAssessment(
    val primaryLevel: LiteracyLevel,
    // Context engineering: deliberate system prompt design, model selection, context curation
    val contextEngineeringScore: Int,
    // Constraint practice: HARNESS.md, deterministic checks, agent-enforced rules
    val constraintScore: Int,
    // Guardrail deployment: hooks, CI checks, automated enforcement of AI collaboration rules
    val guardrailScore: Int,
    // The reasoning model's explanation of this judgment — why this level, what decided it
    val rationale: String,
    val strengths: List<String>,
    val gaps: List<String>
)
