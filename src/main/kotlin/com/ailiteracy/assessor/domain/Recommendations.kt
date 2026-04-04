/**
 * Recommendations holds the 3–5 specific actions the team should take next, generated
 * by the reasoning model after the level assessment is complete. Each recommendation
 * is a [Recommendation] — a concrete action, its expected impact, and a reference to
 * the framework practice it advances.
 *
 * The framework reference is important: it anchors each recommendation to the
 * documented AI Literacy framework so teams can read further, and so future assessments
 * can check whether the recommendation was acted upon by looking for the corresponding
 * framework artefact.
 *
 * Recommendations are ordered by impact — the most valuable next step appears first.
 * This ordering is the recommendation agent's responsibility, not this type's. The type
 * is a pure value object that carries the agent's output.
 *
 * This file does not contain recommendation generation logic. That belongs to the
 * [generateRecommendations] agent action, which reads [LevelAssessment] as input and
 * uses a reasoning model to design an appropriate progression path.
 */
package com.ailiteracy.assessor.domain

/**
 * A single actionable recommendation, anchored to the framework.
 *
 * [description] is the concrete action in plain English — what the team should do.
 * [impact] explains why this action matters — what will improve and by how much.
 * [frameworkReference] is a short string identifying the framework section, skill,
 * appendix, or practice this recommendation advances (e.g. "Appendix B: Harness Design",
 * "Skill: literate-programming", "Level 3: Constraint Practice").
 */
data class Recommendation(
    val description: String,
    val impact: String,
    val frameworkReference: String
)

/**
 * The complete set of recommendations for the assessed team.
 *
 * [items] is ordered by impact — highest-value action first. Consumers should present
 * them in order. The list has 3–5 items by convention; fewer items are permitted when
 * the team is already at a high level with few clear gaps.
 */
data class Recommendations(
    val items: List<Recommendation>
)
