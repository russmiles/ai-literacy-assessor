/**
 * AssessmentRequest is the entry point for every assessment conversation — the minimum
 * information the agent needs to begin. It carries only what the user provides at the
 * start: who is being assessed, where their code lives (if accessible), and whether a
 * previous assessment exists for trajectory comparison.
 *
 * The two optional fields reflect a deliberate design decision: the assessor must work
 * without a repository (for teams with no local access or fully conversational sessions)
 * and without a previous assessment (for first-time assessments). GOAP routing uses
 * the presence or absence of these fields to determine which actions are available —
 * [repoPath] enables [scanRepository], and [previousAssessmentPath] enables
 * [compareTrajectory].
 *
 * This type does not validate paths or parse previous assessment documents. Those
 * responsibilities belong to the scanner and trajectory-comparison actions respectively.
 */
package com.ailiteracy.assessor.domain

/**
 * The initiating request for an ALCI assessment session.
 *
 * [teamName] is required because all outputs — documents, badges, trajectory entries —
 * are keyed to the team, not to an individual or a repository.
 *
 * [repoPath] is absent when the team has no accessible local repository (e.g. running
 * in web-chat mode without a mounted volume). When present it must be a readable path;
 * the scanner will surface errors rather than produce partial evidence.
 *
 * [previousAssessmentPath] points to a previously generated assessment markdown file.
 * The trajectory action reads this file to compute deltas. When absent the trajectory
 * section of the final document reports "first assessment".
 */
data class AssessmentRequest(
    val teamName: String,
    val repoPath: String? = null,
    val previousAssessmentPath: String? = null
)
