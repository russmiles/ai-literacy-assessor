/**
 * ObservableEvidence captures everything the scanner can determine about a repository
 * without asking any questions. It is the product of deterministic file inspection —
 * no LLM is involved in its creation — and serves as the factual foundation for the
 * clarifying questions and level assessment that follow.
 *
 * The design separates raw counts from level signals deliberately. Raw counts (how many
 * skills, how many CI workflows) are neutral facts; level signals are the scanner's
 * interpretation of what those facts imply about literacy level. Keeping them separate
 * allows the assessment agent to accept or override the scanner's interpretation with
 * reasoning, rather than treating machine-detected signals as ground truth.
 *
 * This file does not contain scanning logic. That belongs to RepositoryScanner, which
 * populates this type. ObservableEvidence is purely a value object — immutable evidence
 * ready to be placed on the blackboard.
 */
package com.ailiteracy.assessor.domain

/**
 * The complete set of signals gathered by deterministic repository scanning.
 *
 * Fields are grouped by the framework level they primarily signal, following the
 * design spec's scanning table. Absent features default to zero or false — the
 * assessment agent interprets absence as a gap, not an unknown.
 *
 * [levelSignals] maps a framework level number (0–5) to the list of signal names
 * detected at that level. The assessment agent uses this map to identify which level
 * clusters have corroborating evidence and which are empty.
 */
data class ObservableEvidence(
    // L2 signals: basic engineering hygiene visible in CI configuration
    val ciWorkflowCount: Int = 0,
    val hasTestCoverage: Boolean = false,
    val hasVulnerabilityScanning: Boolean = false,
    val hasMutationTesting: Boolean = false,

    // L3 signals: deliberate AI collaboration habitat
    val hasClaudeMd: Boolean = false,
    val hasHarnessMd: Boolean = false,
    val hasAgentsMd: Boolean = false,
    val hasModelRoutingMd: Boolean = false,
    val hasReflectionLog: Boolean = false,
    val skillCount: Int = 0,
    val agentCount: Int = 0,
    val commandCount: Int = 0,
    val hasHooks: Boolean = false,

    // Constraint counts from HARNESS.md — three tiers of enforcement maturity
    val deterministicConstraintCount: Int = 0,
    val agentConstraintCount: Int = 0,
    val unverifiedConstraintCount: Int = 0,

    // Reflection and learning signals
    val reflectionEntryCount: Int = 0,
    val agentEntryCount: Int = 0,

    // L4 signals: spec-first development practice
    val specCount: Int = 0,
    val planCount: Int = 0,

    // L3+ signals: observability practice
    val observabilitySnapshotCount: Int = 0,

    // The scanner's interpretation: which framework levels have corroborating evidence
    // Key is the level number (0–5); value is the list of named signals found at that level
    val levelSignals: Map<Int, List<String>> = emptyMap()
)
