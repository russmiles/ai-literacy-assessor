/**
 * ALCIDeepDive captures the richly structured responses from Part B of the ALCI
 * instrument. Where the placement instrument gives a rough position, the deep-dive
 * gives texture: how frequently practices are applied, what blocks or enables them,
 * how the team calibrates its own perception against objective signals, and how the
 * team narrates its lived experience.
 *
 * Part B is administered twice: once for the primary level cluster identified by
 * [ALCIPlacement], and once for the adjacent level. This dual administration reveals
 * both what the team has consolidated and what is emerging — the growth edge that
 * recommendations will target.
 *
 * The [summary] method exists because the [assessLevel] action passes this object to
 * an LLM for level judgment. Rather than serialising the entire structure as JSON
 * (which wastes tokens and context), [summary] formats the data as a concise narrative
 * that a reasoning model can interpret efficiently. The format is intentionally
 * human-readable — it mirrors how a skilled assessor would brief a colleague.
 *
 * This type does not contain the question text. That belongs to [DeepDiveInstrument].
 * It also does not contain scoring logic — interpretation is the [assessLevel] action's
 * responsibility.
 */
package com.ailiteracy.assessor.domain

/**
 * The collected responses from ALCI Part B, across both the primary and adjacent
 * level clusters.
 *
 * [practiceFrequency] maps each practice statement to a frequency rating (1 = Never,
 * 5 = Always). [blockerEnablerPairs] maps each paired item label to a rating (1–5)
 * where low scores indicate a blocker and high scores an enabler. [quantitativeSelfReport]
 * maps short labels to numeric values (e.g. "% of PRs reviewed by AI" → 40).
 * [calibrationItems] map calibration statement text to a boolean: true means the team's
 * perception aligns with the objective signal, false means misalignment.
 * [openEndedResponses] hold free-form prose answers to the one open question per level.
 */
data class ALCIDeepDive(
    val practiceFrequency: Map<String, Int> = emptyMap(),
    val blockerEnablerPairs: Map<String, Int> = emptyMap(),
    val quantitativeSelfReport: Map<String, Number> = emptyMap(),
    val calibrationItems: Map<String, Boolean> = emptyMap(),
    val openEndedResponses: List<String> = emptyList()
) {
    /**
     * Formats this deep-dive as a concise, human-readable brief for LLM consumption.
     *
     * The format is designed for a reasoning model, not for machine parsing. It presents
     * high-frequency practices first (the strongest positive signals), then low-frequency
     * practices (gaps), then blocker/enabler balance, then quantitative anchors, then
     * calibration alignment, and finally the open narrative. This ordering mirrors how
     * a skilled assessor would summarise the evidence — strengths before gaps, numbers
     * before stories.
     */
    fun summary(): String {
        val sb = StringBuilder()

        if (practiceFrequency.isNotEmpty()) {
            val sorted = practiceFrequency.entries.sortedByDescending { it.value }
            sb.appendLine("Practice frequency (1=Never, 5=Always):")
            sorted.forEach { (practice, rating) ->
                sb.appendLine("  $practice: $rating/5")
            }
        }

        if (blockerEnablerPairs.isNotEmpty()) {
            sb.appendLine("Blocker/enabler pairs (1=strong blocker, 5=strong enabler):")
            blockerEnablerPairs.forEach { (item, rating) ->
                val label = when {
                    rating <= 2 -> "blocker"
                    rating >= 4 -> "enabler"
                    else -> "neutral"
                }
                sb.appendLine("  $item: $rating/5 ($label)")
            }
        }

        if (quantitativeSelfReport.isNotEmpty()) {
            sb.appendLine("Quantitative self-report:")
            quantitativeSelfReport.forEach { (label, value) ->
                sb.appendLine("  $label: $value")
            }
        }

        if (calibrationItems.isNotEmpty()) {
            val aligned = calibrationItems.count { it.value }
            val total = calibrationItems.size
            sb.appendLine("Perception-reality calibration: $aligned/$total items aligned")
            calibrationItems.filter { !it.value }.forEach { (item, _) ->
                sb.appendLine("  Misaligned: $item")
            }
        }

        if (openEndedResponses.isNotEmpty()) {
            sb.appendLine("Open-ended responses:")
            openEndedResponses.forEachIndexed { i, response ->
                sb.appendLine("  [${i + 1}] $response")
            }
        }

        return sb.toString().trimEnd()
    }
}
