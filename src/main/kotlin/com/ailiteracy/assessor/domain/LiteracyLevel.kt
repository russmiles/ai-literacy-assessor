/**
 * LiteracyLevel is the foundational vocabulary of the AI Literacy Collaboration Index.
 * It defines the six stages of an engineering team's progression from passive awareness
 * of AI tools to the highest level of deliberate, sovereign practice — where the team
 * shapes its own AI collaboration habitat rather than being shaped by default behaviours.
 *
 * The six levels are ordered and load-bearing: every assessment, recommendation, and
 * trajectory comparison is expressed in terms of movement between these levels. The
 * [number] field matches the framework's L0–L5 shorthand so that references in
 * assessment documents, skill files, and CI annotations are unambiguous.
 *
 * This file does not contain scoring logic, transition criteria, or instrument questions —
 * those belong to the ALCI instrument and assessment agent respectively.
 */
package com.ailiteracy.assessor.domain

/**
 * The six stages of AI literacy as defined by the AI Literacy Collaboration Index.
 *
 * Each level has a numeric identifier (matching the framework's L0–L5 notation) and
 * a human-readable display name used in assessment documents and progress communications.
 * The ordering from UNAWARE to SOVEREIGN_ENGINEER is intentional: lower ordinals
 * represent earlier stages, so ordinal comparisons are meaningful for trajectory logic.
 */
enum class LiteracyLevel(val number: Int, val displayName: String) {
    UNAWARE(0, "Unaware"),
    AWARE(1, "Aware"),
    EXPERIMENTING(2, "Experimenting"),
    PRACTISING(3, "Practising"),
    INTEGRATING(4, "Integrating"),
    SOVEREIGN_ENGINEER(5, "Sovereign Engineer")
}
