/**
 * ALCIPlacement records the raw scores from Part A of the ALCI instrument — the ten
 * placement statements — and computes the team's primary level cluster from those scores.
 *
 * Part A works by pairing two statements per level cluster (L0–L4). The team rates each
 * statement 1–5. The average per cluster determines which level cluster has the highest
 * agreement, and that cluster becomes the primary level. Level 5 (Sovereign Engineer)
 * is not assessed through placement statements — it requires the deep-dive evidence.
 *
 * The scores map uses level number as key (0–4) and a two-element list of ratings as
 * value. This mirrors the instrument structure directly: two statements per level, no
 * more. The [primaryLevel] and [adjacentLevel] are computed from the scores by the
 * [administerPlacement] action before this type is placed on the blackboard — they are
 * not derived lazily, because the deep-dive action needs a stable level target to know
 * which questions to administer.
 *
 * This type does not contain the statement text. That belongs to [PlacementInstrument].
 */
package com.ailiteracy.assessor.domain

/**
 * The scored output of ALCI Part A: ten placement statements, computed primary level,
 * and the adjacent level that will also receive the deep-dive instrument.
 *
 * [scores] maps each level cluster (0–4) to the two ratings given by the team. Each
 * rating is in range 1–5. The map always contains all five keys after the placement
 * instrument has been administered.
 *
 * [primaryLevel] is the level cluster with the highest average score. [adjacentLevel]
 * is the next level up (or down if the primary is already at L4), used to administer
 * the deep-dive for growth edge identification.
 */
data class ALCIPlacement(
    val scores: Map<Int, List<Int>>,
    val primaryLevel: LiteracyLevel,
    val adjacentLevel: LiteracyLevel
)
