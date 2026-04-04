/**
 * DeepDiveInstrument holds the ALCI Part B and Part C question sets.
 *
 * Where Part A locates a team on the literacy spectrum through placement
 * statements, Part B goes deep: it asks about practice frequency, surfaces
 * the forces that block or enable each practice, anchors the team's
 * self-assessment in quantitative signals, and invites open reflection.
 * Part C gathers three lived-experience narratives that give the assessing
 * agent qualitative texture beyond what ratings can capture.
 *
 * The deep-dive is administered at two levels: the primary level identified
 * by Part A and the adjacent level. This dual administration is what reveals
 * both consolidation (primary level evidence) and the growth edge (adjacent
 * level signals). The [getPartBQuestions] method returns the question set for
 * whichever level the agent is currently administering.
 *
 * This class holds only question text and structure — no response data. That
 * belongs to ALCIDeepDive, which the administerDeepDive action populates as
 * it collects responses. Keeping questions and responses separate prevents this
 * class from accumulating mutable state across assessment sessions.
 *
 * Part C questions are level-independent: they ask for narrative regardless of
 * where the team sits. They are returned by [getPartCQuestions].
 */
package com.ailiteracy.assessor.alci

import com.ailiteracy.assessor.domain.LiteracyLevel
import org.springframework.stereotype.Component

/**
 * A structured question from the ALCI deep-dive instrument.
 *
 * [text] is the question as it would be read to or displayed for the team.
 * [category] classifies the question type so the agent can route responses
 * to the correct field of ALCIDeepDive. [label] is the short key used as
 * the map key when storing the response.
 */
data class DeepDiveQuestion(
    val text: String,
    val category: QuestionCategory,
    val label: String
)

/**
 * The four categories of Part B question, plus the narrative category for
 * Part C. The category determines which ALCIDeepDive field receives the
 * response, so the admin action can build the deep-dive object cleanly.
 */
enum class QuestionCategory {
    PRACTICE_FREQUENCY,
    BLOCKER_ENABLER,
    QUANTITATIVE_SELF_REPORT,
    OPEN_ENDED
}

/**
 * The ALCI Part B and Part C instrument: level-specific deep-dive questions
 * and three universal lived-experience prompts.
 *
 * [getPartBQuestions] returns the question list for a given level. The list
 * is ordered: frequency items first (they anchor the frame), then
 * blocker/enabler pairs (they explain why frequency is what it is), then
 * quantitative self-report (numerical calibration), then the single open
 * question (narrative synthesis). [getPartCQuestions] returns the three
 * Part C prompts, which are administered once regardless of level.
 */
@Component
class DeepDiveInstrument {

    // Part B question sets, keyed by literacy level number (0–4).
    // Each set follows the same structure: frequency → blocker/enabler →
    // quantitative → open-ended. The open-ended question is always last
    // because it invites synthesis after the team has already reflected
    // through the structured items.
    private val partBByLevel: Map<Int, List<DeepDiveQuestion>> = mapOf(

        // L0 — UNAWARE: questions probe conceptual grasp and exposure level
        0 to listOf(
            DeepDiveQuestion(
                text = "How often do you read or watch material about AI capabilities and limitations — not marketing, but technical or practitioner sources?",
                category = QuestionCategory.PRACTICE_FREQUENCY,
                label = "ai-learning-frequency"
            ),
            DeepDiveQuestion(
                text = "When you encounter an AI claim (in a blog post, a vendor deck, a conference talk), how often do you look for independent verification before accepting it?",
                category = QuestionCategory.PRACTICE_FREQUENCY,
                label = "claim-verification-frequency"
            ),
            DeepDiveQuestion(
                text = "Rate how much each factor blocks or enables your team's engagement with AI: time pressure (1=strong blocker, 5=strong enabler).",
                category = QuestionCategory.BLOCKER_ENABLER,
                label = "time-pressure"
            ),
            DeepDiveQuestion(
                text = "Rate how much each factor blocks or enables: scepticism about AI value within the team.",
                category = QuestionCategory.BLOCKER_ENABLER,
                label = "team-scepticism"
            ),
            DeepDiveQuestion(
                text = "Roughly how many hours per month does your team spend deliberately learning about AI (courses, reading, experiments) — not incidental use?",
                category = QuestionCategory.QUANTITATIVE_SELF_REPORT,
                label = "monthly-learning-hours"
            ),
            DeepDiveQuestion(
                text = "Describe a recent situation where AI did not live up to its promise for your team. What did you learn from it?",
                category = QuestionCategory.OPEN_ENDED,
                label = "ai-disappointment-narrative"
            )
        ),

        // L1 — AWARE: questions probe prompt craft and intentional interaction
        1 to listOf(
            DeepDiveQuestion(
                text = "How often do you decompose a complex request into a sequence of focused prompts rather than sending one large prompt?",
                category = QuestionCategory.PRACTICE_FREQUENCY,
                label = "prompt-decomposition-frequency"
            ),
            DeepDiveQuestion(
                text = "How often do you review and refine an AI response iteratively in the same session, rather than accepting the first output?",
                category = QuestionCategory.PRACTICE_FREQUENCY,
                label = "iterative-refinement-frequency"
            ),
            DeepDiveQuestion(
                text = "Rate: context window management (knowing what's in context, trimming noise) — blocker or enabler for your prompt quality?",
                category = QuestionCategory.BLOCKER_ENABLER,
                label = "context-management"
            ),
            DeepDiveQuestion(
                text = "Rate: team shared vocabulary for prompting — do you have shared patterns, or does everyone improvise independently?",
                category = QuestionCategory.BLOCKER_ENABLER,
                label = "shared-prompting-vocabulary"
            ),
            DeepDiveQuestion(
                text = "What percentage of your team's AI interactions produce a useful output on the first or second attempt (without major rework)?",
                category = QuestionCategory.QUANTITATIVE_SELF_REPORT,
                label = "first-attempt-success-rate"
            ),
            DeepDiveQuestion(
                text = "Describe the most effective prompt pattern your team has developed. What makes it work?",
                category = QuestionCategory.OPEN_ENDED,
                label = "effective-prompt-pattern-narrative"
            )
        ),

        // L2 — EXPERIMENTING: questions probe verification discipline and test coverage
        2 to listOf(
            DeepDiveQuestion(
                text = "How often do you write or run tests before accepting AI-generated code into the codebase?",
                category = QuestionCategory.PRACTICE_FREQUENCY,
                label = "test-before-accept-frequency"
            ),
            DeepDiveQuestion(
                text = "How often does your CI pipeline catch regressions introduced by AI-generated changes?",
                category = QuestionCategory.PRACTICE_FREQUENCY,
                label = "ci-regression-catch-frequency"
            ),
            DeepDiveQuestion(
                text = "Rate: test coverage threshold enforcement — does your CI gate on coverage, and does that gate hold?",
                category = QuestionCategory.BLOCKER_ENABLER,
                label = "coverage-enforcement"
            ),
            DeepDiveQuestion(
                text = "Rate: AI hallucination rate in your domain — how often does AI-generated code contain factually wrong API calls or logic errors?",
                category = QuestionCategory.BLOCKER_ENABLER,
                label = "hallucination-rate"
            ),
            DeepDiveQuestion(
                text = "What is your current test coverage percentage, and has it changed since you started using AI assistants?",
                category = QuestionCategory.QUANTITATIVE_SELF_REPORT,
                label = "test-coverage-percent"
            ),
            DeepDiveQuestion(
                text = "Describe a time when your verification process caught something important that AI got wrong. What was the error and how did testing reveal it?",
                category = QuestionCategory.OPEN_ENDED,
                label = "verification-catch-narrative"
            )
        ),

        // L3 — PRACTISING: questions probe habitat engineering and constraint enforcement
        3 to listOf(
            DeepDiveQuestion(
                text = "How often do you update your CLAUDE.md (or equivalent) when team conventions change?",
                category = QuestionCategory.PRACTICE_FREQUENCY,
                label = "claudemd-update-frequency"
            ),
            DeepDiveQuestion(
                text = "How often do you find AI behaviour drifting away from your documented conventions, requiring correction?",
                category = QuestionCategory.PRACTICE_FREQUENCY,
                label = "convention-drift-frequency"
            ),
            DeepDiveQuestion(
                text = "Rate: constraint coverage — what fraction of your team's important working agreements are enforced by hooks, CI, or agents (not just documented)?",
                category = QuestionCategory.BLOCKER_ENABLER,
                label = "constraint-coverage"
            ),
            DeepDiveQuestion(
                text = "Rate: tooling friction — how much does setting up and maintaining hooks and agents slow the team down versus the value they provide?",
                category = QuestionCategory.BLOCKER_ENABLER,
                label = "tooling-friction"
            ),
            DeepDiveQuestion(
                text = "How many active constraints does your HARNESS.md (or equivalent) document? How many are enforced deterministically, and how many rely on agent review?",
                category = QuestionCategory.QUANTITATIVE_SELF_REPORT,
                label = "constraint-counts"
            ),
            DeepDiveQuestion(
                text = "Describe how your habitat has evolved over the past three months. What did you add, what did you remove, and what surprised you?",
                category = QuestionCategory.OPEN_ENDED,
                label = "habitat-evolution-narrative"
            )
        ),

        // L4 — INTEGRATING: questions probe specification-first discipline
        4 to listOf(
            DeepDiveQuestion(
                text = "How often do you write a spec or acceptance criteria before asking AI to generate implementation code?",
                category = QuestionCategory.PRACTICE_FREQUENCY,
                label = "spec-first-frequency"
            ),
            DeepDiveQuestion(
                text = "How often do you regenerate or significantly restructure AI-generated code from a spec rather than patching it incrementally?",
                category = QuestionCategory.PRACTICE_FREQUENCY,
                label = "regeneration-frequency"
            ),
            DeepDiveQuestion(
                text = "Rate: spec quality — how often does a spec you write produce AI output that passes acceptance criteria without significant rework?",
                category = QuestionCategory.BLOCKER_ENABLER,
                label = "spec-quality"
            ),
            DeepDiveQuestion(
                text = "Rate: organisational trust — does your organisation treat AI-generated code with the same review rigour as human-written code?",
                category = QuestionCategory.BLOCKER_ENABLER,
                label = "organisational-trust"
            ),
            DeepDiveQuestion(
                text = "What percentage of new features on your current project started with a written spec before any code was generated?",
                category = QuestionCategory.QUANTITATIVE_SELF_REPORT,
                label = "spec-first-rate"
            ),
            DeepDiveQuestion(
                text = "Describe your specification workflow. How do you move from a problem statement to a spec that AI can act on reliably?",
                category = QuestionCategory.OPEN_ENDED,
                label = "specification-workflow-narrative"
            )
        )
    )

    // Part C questions are level-independent. They ask for narrative evidence
    // of lived experience — moments that reveal more than ratings can. Three
    // questions, administered once per assessment, always in this order.
    private val partCQuestions: List<DeepDiveQuestion> = listOf(
        DeepDiveQuestion(
            text = "Describe the moment your team's relationship with AI tools changed significantly — what happened, and what shifted?",
            category = QuestionCategory.OPEN_ENDED,
            label = "turning-point-narrative"
        ),
        DeepDiveQuestion(
            text = "What is the most important thing your team has learned about working with AI that you wish you had known at the start?",
            category = QuestionCategory.OPEN_ENDED,
            label = "key-learning-narrative"
        ),
        DeepDiveQuestion(
            text = "Where do you want your team to be in its AI literacy in twelve months, and what is the single biggest obstacle to getting there?",
            category = QuestionCategory.OPEN_ENDED,
            label = "aspiration-obstacle-narrative"
        )
    )

    /**
     * Returns the Part B question set for the given literacy level.
     *
     * The level passed here should be the primary or adjacent level from
     * ALCIPlacement, not an arbitrary number. If a level has no question set
     * (e.g. SOVEREIGN_ENGINEER, which has no Part A statements), the method
     * returns the L4 set as the closest proxy — sovereign engineer evidence
     * is gathered through L4 integration questions plus the Part C narratives.
     */
    fun getPartBQuestions(level: LiteracyLevel): List<DeepDiveQuestion> {
        // SOVEREIGN_ENGINEER (5) falls back to L4 because there are no L5
        // placement statements. Its evidence comes from L4 depth plus narrative.
        val levelKey = minOf(level.number, 4)
        return partBByLevel[levelKey] ?: partBByLevel[4]!!
    }

    /**
     * Returns the three Part C lived-experience questions.
     *
     * These are administered once per assessment regardless of level. They
     * are kept separate from Part B so the agent can administer them at the
     * end of the session, after the structured ratings have primed the team's
     * reflection.
     */
    fun getPartCQuestions(): List<DeepDiveQuestion> = partCQuestions
}
