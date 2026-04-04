/**
 * ClarifyingQuestions holds the 3–5 questions the agent generates after scanning
 * the repository (or after noting its absence). Their purpose is to fill the gaps
 * that file inspection cannot address: how the team actually uses the tools present,
 * whether practices are habitual or experimental, and what the team's subjective
 * experience of AI collaboration has been.
 *
 * The questions are tailored to the evidence already gathered — a repo with no
 * CLAUDE.md warrants different questions than one with a full harness. This tailoring
 * happens in the [generateQuestions] action; by the time this type is on the blackboard
 * the questions are already context-specific.
 *
 * This type carries only the question strings, not the structure of the expected
 * answers. The answers are free-form prose captured in [ClarifyingResponses]. This
 * separation allows the questions to be displayed to the user and answered at their
 * own pace before any scoring logic runs.
 */
package com.ailiteracy.assessor.domain

/**
 * A short list of open questions tailored to gaps in the observable evidence.
 *
 * [questions] is ordered from most diagnostic to least — the agent places the question
 * most likely to shift the level assessment first, so that time-constrained users get
 * maximum value from answering even one or two.
 */
data class ClarifyingQuestions(
    val questions: List<String>
)
