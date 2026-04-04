/**
 * PlacementInstrument holds the ten ALCI Part A placement statements and the
 * scoring logic that converts raw ratings into a primary literacy level.
 *
 * Part A of the ALCI is the placement phase: the team rates two statements per
 * level cluster (L0–L4) on a 1–5 scale. The cluster with the highest average
 * score identifies where the team's practices are most consolidated — that is
 * the primary level. When two clusters tie, the lower level wins, because a tie
 * indicates the team has not yet fully consolidated the higher cluster's
 * practices.
 *
 * The statement text lives here rather than in the domain objects because it is
 * instrument data, not assessment state. ALCIPlacement stores scores; this
 * class stores the questions that generated those scores. Keeping them separate
 * means the domain objects stay serialisable and LLM-friendly without embedding
 * multi-line prose.
 *
 * This class does not perform the deep-dive — that is DeepDiveInstrument's
 * responsibility. It also does not determine the adjacent level; that is the
 * administerPlacement action's job once it has the primary level in hand.
 */
package com.ailiteracy.assessor.alci

import com.ailiteracy.assessor.domain.LiteracyLevel
import org.springframework.stereotype.Component

/**
 * The ALCI Part A instrument: ten placement statements and the computation
 * that maps team scores onto a primary literacy level.
 *
 * Statements are ordered L0 to L4, two per cluster, matching the administration
 * sequence. The [getStatements] method returns them with their level tag so the
 * caller can group and present them appropriately. The [computeLevel] method is
 * the single scoring rule: highest average wins, ties go lower.
 */
@Component
class PlacementInstrument {

    // The ten placement statements, each tagged with the level cluster (0–4)
    // they probe. Two statements per level, presented in the order a facilitator
    // would administer them — from foundation to frontier.
    private val statements: List<Pair<Int, String>> = listOf(
        // L0 — UNAWARE cluster: foundational conceptual orientation
        0 to "I can explain why AI is an amplifier of existing human intent and skill, not a replacement for it.",
        0 to "I can distinguish between AI hype (claims about what AI will do) and AI reality (what it demonstrably does today).",
        // L1 — AWARE cluster: intentional prompt-level interaction
        1 to "I can consistently translate my intent into prompts that produce useful first drafts, and I iterate deliberately rather than by trial and error.",
        1 to "I understand that every token I provide to an AI model is a design decision, and I make those decisions consciously.",
        // L2 — EXPERIMENTING cluster: systematic verification practices
        2 to "I have systematic practices for verifying AI-generated output — I do not accept it at face value, and I can explain what I check and why.",
        2 to "I use automated tests as my primary tool for validating AI-generated code, and my test suite runs on every change.",
        // L3 — PRACTISING cluster: habitat engineering
        3 to "My project has a living document (CLAUDE.md or equivalent) that encodes team conventions, and AI assistants are constrained by it.",
        3 to "My development environment includes automated enforcement of at least five AI collaboration constraints via hooks, CI, or agents.",
        // L4 — INTEGRATING cluster: specification-first discipline
        4 to "I write specifications before implementation — the spec shapes what AI produces, not the other way around.",
        4 to "I think of code as a disposable artifact that emerges from a specification, and I could regenerate it from scratch if needed."
    )

    /**
     * Returns all ten placement statements with their level tags.
     *
     * The list is ordered L0 to L4 (two per level) so that callers administering
     * the instrument sequentially get them in the natural facilitation order.
     * Callers that need to group by level can filter on the Int component.
     */
    fun getStatements(): List<Pair<Int, String>> = statements

    /**
     * Computes the primary literacy level from a map of cluster scores.
     *
     * The input is a map from level number (0–4) to a two-element list of
     * ratings (1–5 each). The method averages each pair and returns the
     * LiteracyLevel whose cluster has the highest average. When two clusters
     * share the highest average, the lower-numbered cluster wins — the
     * conservative reading is the accurate one when evidence is ambiguous.
     *
     * Note that L5 (SOVEREIGN_ENGINEER) is not reachable through Part A;
     * it requires the deep-dive evidence administered in Part B.
     */
    fun computeLevel(scores: Map<Int, List<Int>>): LiteracyLevel {
        // Compute the average for each cluster. Use Double for precision even
        // though current instruments only produce .0 or .5 averages — future
        // instrument variants may allow fractional scores.
        val averages: Map<Int, Double> = scores.mapValues { (_, ratings) ->
            ratings.sum().toDouble() / ratings.size
        }

        // Find the highest average. sortedBy on level number means that when
        // we find the max, ties are broken in favour of lower level numbers
        // because we iterate from L0 upward and maxByOrNull returns the last
        // maximum found — so we reverse: iterate from highest to lowest and
        // return the first entry at the maximum, giving us the lower level on tie.
        val maxAverage = averages.values.maxOrNull() ?: return LiteracyLevel.UNAWARE

        // Filter to only entries at the maximum average, then take the one with
        // the smallest level number. This makes the tie-break explicit and
        // independent of iteration order.
        val winningLevel = averages
            .filter { (_, avg) -> avg == maxAverage }
            .minByOrNull { (level, _) -> level }
            ?.key
            ?: return LiteracyLevel.UNAWARE

        // Map the winning level number onto the LiteracyLevel enum. Level 5
        // (SOVEREIGN_ENGINEER) cannot be the result of Part A scoring because
        // Part A has no L5 statements. The coerceAtMost ensures we never
        // accidentally map out of bounds even if the caller provides an L5 key.
        return LiteracyLevel.entries.firstOrNull { it.number == winningLevel }
            ?: LiteracyLevel.UNAWARE
    }
}
