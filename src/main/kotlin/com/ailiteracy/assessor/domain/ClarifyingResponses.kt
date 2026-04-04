/**
 * ClarifyingResponses pairs each clarifying question with the team's answer. It is
 * produced by the [collectResponses] action, which presents the questions to the user
 * through whatever interface is active (CLI or web chat) and collects free-form prose.
 *
 * The map structure — question text as key, answer as value — keeps the question and
 * answer together as a unit. This matters for the assessment agent, which must reason
 * about each question-answer pair in context: knowing both what was asked and what was
 * said is necessary to interpret the response correctly.
 *
 * This type does not validate or score responses. Those responsibilities belong to the
 * [assessLevel] action, which reads both [ObservableEvidence] and [ALCIDeepDive]
 * alongside these responses when forming its judgment.
 */
package com.ailiteracy.assessor.domain

/**
 * The team's free-form answers to the tailored clarifying questions.
 *
 * [responses] maps each question string (from [ClarifyingQuestions]) to the team's
 * answer. The map may be partial if the team skipped questions — absent keys are
 * treated as unanswered, not as negative signals.
 */
data class ClarifyingResponses(
    val responses: Map<String, String>
)
