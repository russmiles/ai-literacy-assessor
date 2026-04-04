/**
 * AssessmentDocument is the final output of the assessment workflow: a complete,
 * timestamped markdown document ready to write to disk. The [markdownContent] field
 * contains everything — level badge, discipline scores, rationale, strengths, gaps,
 * recommendations, and trajectory — formatted as a human-readable report.
 *
 * The metadata fields ([teamName], [assessmentDate], [level]) are stored alongside the
 * markdown for two reasons. First, the [writeAssessment] action uses them to construct
 * the output filename (e.g. `assessments/2026-04-03-team-name.md`) without parsing the
 * document it just assembled. Second, trajectory comparison in future assessments can
 * read these fields to quickly locate level and date without parsing the full markdown.
 *
 * The document is assembled by the [assembleDocument] action, which uses a balanced
 * model to synthesise [LevelAssessment], [Recommendations], and [TrajectoryComparison]
 * into coherent prose. This type does not contain assembly logic — it is a pure value
 * that carries the result.
 *
 * The markdown format is the canonical output format. There is no PDF or HTML output —
 * markdown is readable in any text editor, versionable in git, and renderable by any
 * modern documentation platform.
 */
package com.ailiteracy.assessor.domain

import java.time.LocalDate

/**
 * The assembled assessment document, ready for the write action.
 *
 * [markdownContent] is the complete document as a markdown string. It includes all
 * sections: summary badge, level assessment with rationale, discipline scores,
 * strengths, gaps, recommendations, trajectory comparison, and metadata footer.
 *
 * [assessmentDate] defaults to today, set at assembly time. Future trajectory comparisons
 * will read this date to compute how much time elapsed between assessments.
 */
data class AssessmentDocument(
    val markdownContent: String,
    val teamName: String,
    val assessmentDate: LocalDate = LocalDate.now(),
    val level: LiteracyLevel
)
