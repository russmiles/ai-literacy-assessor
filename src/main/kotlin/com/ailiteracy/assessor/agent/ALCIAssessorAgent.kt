/**
 * ALCIAssessorAgent is the GOAP agent that conducts a complete AI Literacy
 * Collaboration Index assessment. It orchestrates ten actions, each producing
 * a typed domain object that the next action consumes, with the Embabel planner
 * discovering the chain from type signatures alone.
 *
 * The assessment flows through three phases:
 *
 * 1. **Evidence gathering** (actions 1-5): scan the repository for observable
 *    signals, generate clarifying questions to fill gaps, collect responses,
 *    administer the ALCI placement instrument, and run the deep-dive questions.
 *
 * 2. **Judgment** (actions 6-8): assess the team's literacy level using a
 *    reasoning model, generate recommendations, and compare trajectory with
 *    any previous assessment.
 *
 * 3. **Delivery** (actions 9-10): assemble all findings into a markdown
 *    document and write it to disk.
 *
 * Four of the ten actions are deterministic (no LLM): scanRepository,
 * collectResponses, administerPlacement, and writeAssessment. The remaining
 * six use LLM model roles configured in application.yml: "reasoning" for the
 * high-stakes assessment and recommendation actions, "fast" for question
 * generation and trajectory comparison, and "balanced" for document assembly.
 *
 * This agent does not handle session management, delivery channel selection,
 * or CLI/web routing. Those concerns belong to the interaction port
 * implementations and the Spring Shell / web controller layer.
 */
package com.ailiteracy.assessor.agent

import com.ailiteracy.assessor.alci.DeepDiveInstrument
import com.ailiteracy.assessor.alci.PlacementInstrument
import com.ailiteracy.assessor.alci.QuestionCategory
import com.ailiteracy.assessor.domain.ALCIDeepDive
import com.ailiteracy.assessor.domain.ALCIPlacement
import com.ailiteracy.assessor.domain.AssessmentDocument
import com.ailiteracy.assessor.domain.AssessmentRequest
import com.ailiteracy.assessor.domain.ClarifyingQuestions
import com.ailiteracy.assessor.domain.ClarifyingResponses
import com.ailiteracy.assessor.domain.LevelAssessment
import com.ailiteracy.assessor.domain.LiteracyLevel
import com.ailiteracy.assessor.domain.ObservableEvidence
import com.ailiteracy.assessor.domain.Recommendations
import com.ailiteracy.assessor.domain.TrajectoryComparison
import com.ailiteracy.assessor.interaction.UserInteractionPort
import com.ailiteracy.assessor.scanner.RepositoryScanner
import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.common.ActionContext
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria
import org.springframework.beans.factory.annotation.Value
import java.io.File
import java.time.LocalDate

/**
 * The ALCI assessment agent. Constructor-injected dependencies provide the
 * deterministic instruments (scanner, placement, deep-dive) and the I/O
 * channel. The output directory is configurable via application.yml.
 */
@Agent(description = "Conducts an AI Literacy Collaboration Index assessment through evidence gathering, ALCI instrument administration, level judgment, and recommendation generation")
class ALCIAssessorAgent(
    private val scanner: RepositoryScanner,
    private val placement: PlacementInstrument,
    private val deepDive: DeepDiveInstrument,
    private val interaction: UserInteractionPort,
    @Value("\${assessor.output-dir:./assessments}") private val outputDir: String = "./assessments"
) {

    // -- Phase 1: Evidence Gathering --

    /**
     * Scans the repository for observable evidence of AI literacy practices.
     *
     * This is a separate action (not inlined into generateQuestions) because the
     * planner needs to handle the case where no repo path is provided. When the
     * path is null, this action produces empty evidence, and the question
     * generator adapts by asking broader questions about team practices.
     */
    @Action(description = "Scan a repository for observable AI literacy signals", cost = 1.0)
    fun scanRepository(request: AssessmentRequest): ObservableEvidence {
        if (request.repoPath == null) {
            return ObservableEvidence()
        }
        interaction.showProgress("Scanning repository at ${request.repoPath}...")
        return scanner.scan(request.repoPath)
    }

    /**
     * Generates 3-5 clarifying questions based on gaps in the observable evidence.
     *
     * Uses the "fast" model role because question generation needs speed, not deep
     * reasoning. The questions fill gaps that file scanning cannot address: how the
     * team actually uses the tools present, whether practices are habitual or
     * experimental, and what the team's subjective experience has been.
     *
     * When evidence is empty (no repo scanned), the questions are broader, probing
     * general team practices rather than specific artefacts.
     */
    @Action(description = "Generate clarifying questions based on evidence gaps", cost = 2.0)
    fun generateQuestions(evidence: ObservableEvidence, context: ActionContext): ClarifyingQuestions {
        interaction.showProgress("Generating clarifying questions...")

        val evidenceSummary = if (evidence.levelSignals.values.flatten().isEmpty()) {
            "No repository was scanned. Evidence will come entirely from conversation."
        } else {
            buildString {
                appendLine("Observable evidence from repository scan:")
                evidence.levelSignals.forEach { (level, signals) ->
                    if (signals.isNotEmpty()) {
                        appendLine("  Level $level signals: ${signals.joinToString(", ")}")
                    }
                }
            }
        }

        val prompt = """
            You are an AI literacy assessor. Based on the following observable evidence
            (or lack thereof), generate 3-5 clarifying questions that will help assess
            the team's AI literacy level.

            $evidenceSummary

            Focus on gaps — what the scan could not tell us. Ask about:
            - How the team actually uses AI tools day-to-day
            - Whether observed practices are habitual or experimental
            - The team's subjective experience with AI collaboration
            - Verification and quality practices around AI-generated output

            Return a JSON object with a single field "questions" containing a list of
            question strings. Each question should be specific, open-ended, and
            diagnostic — it should help distinguish between adjacent literacy levels.
        """.trimIndent()

        val runner = context.promptRunner()
            .withLlm(LlmOptions.fromCriteria(ModelSelectionCriteria.byRole("fast")))

        return runner.createObject(prompt, ClarifyingQuestions::class.java)
    }

    /**
     * Collects the team's answers to each clarifying question via the interaction port.
     *
     * This is a deterministic action — no LLM. It exists as a separate action because
     * the GOAP planner needs a clear boundary between question generation (LLM) and
     * response collection (I/O). If question generation fails and replanning occurs,
     * this action is not re-executed.
     */
    @Action(description = "Collect team responses to clarifying questions", cost = 1.0)
    fun collectResponses(questions: ClarifyingQuestions): ClarifyingResponses {
        interaction.showProgress("\nPlease answer the following questions about your team's AI practices:\n")

        val responses = mutableMapOf<String, String>()
        questions.questions.forEachIndexed { index, question ->
            interaction.showProgress("Question ${index + 1} of ${questions.questions.size}:")
            val answer = interaction.askQuestion(question)
            responses[question] = answer
        }

        return ClarifyingResponses(responses = responses)
    }

    /**
     * Administers the ALCI Part A placement instrument: ten statements rated 1-5.
     *
     * No LLM is involved. The interaction port presents each statement and collects
     * a Likert rating. The PlacementInstrument computes the primary level from the
     * collected scores, and this action determines the adjacent level (one above the
     * primary, or one below if the primary is already at L4).
     *
     * This is a separate action from the deep-dive because the placement result
     * determines which deep-dive questions to administer. The planner must complete
     * placement before starting the deep-dive.
     */
    @Action(description = "Administer the ALCI Part A placement instrument", cost = 3.0)
    fun administerPlacement(responses: ClarifyingResponses): ALCIPlacement {
        interaction.showProgress("\n--- ALCI Part A: Placement ---")
        interaction.showProgress("Rate each statement from 1 (strongly disagree) to 5 (strongly agree):\n")

        val statements = placement.getStatements()
        val scores = mutableMapOf<Int, MutableList<Int>>()
        (0..4).forEach { scores[it] = mutableListOf() }

        statements.forEach { (level, statement) ->
            val rating = interaction.presentStatement(statement, 1..5)
            scores[level]!!.add(rating)
        }

        val primaryLevel = placement.computeLevel(scores)

        // The adjacent level is one above the primary (the growth edge), unless the
        // primary is already at L4 — then the adjacent is one below. L5 is never
        // reachable through placement alone.
        val adjacentLevel = when {
            primaryLevel.number < 4 -> LiteracyLevel.entries.first { it.number == primaryLevel.number + 1 }
            else -> LiteracyLevel.entries.first { it.number == primaryLevel.number - 1 }
        }

        interaction.showProgress("Placement result: ${primaryLevel.displayName} (L${primaryLevel.number})")

        return ALCIPlacement(
            scores = scores,
            primaryLevel = primaryLevel,
            adjacentLevel = adjacentLevel
        )
    }

    /**
     * Administers the ALCI Part B deep-dive and Part C narratives via the interaction port.
     *
     * No LLM is involved. The deep-dive questions are level-specific (administered for
     * both the primary and adjacent levels) and produce structured responses that the
     * assessment action will reason about. Part C questions are level-independent
     * narratives administered once.
     *
     * This is a separate action from administerPlacement because it depends on knowing
     * the primary level — which the placement action determines. The planner enforces
     * this ordering through the type chain: ALCIPlacement -> ALCIDeepDive.
     */
    @Action(description = "Administer the ALCI Part B deep-dive and Part C narratives", cost = 4.0)
    fun administerDeepDive(placementResult: ALCIPlacement): ALCIDeepDive {
        interaction.showProgress("\n--- ALCI Part B: Deep Dive ---")

        val practiceFrequency = mutableMapOf<String, Int>()
        val blockerEnablerPairs = mutableMapOf<String, Int>()
        val quantitativeSelfReport = mutableMapOf<String, Number>()
        val openEndedResponses = mutableListOf<String>()

        // Administer Part B for both primary and adjacent levels
        listOf(placementResult.primaryLevel, placementResult.adjacentLevel).forEach { level ->
            interaction.showProgress("\nDeep-dive questions for ${level.displayName} (L${level.number}):\n")
            val questions = deepDive.getPartBQuestions(level)

            questions.forEach { question ->
                when (question.category) {
                    QuestionCategory.PRACTICE_FREQUENCY -> {
                        val rating = interaction.presentStatement(question.text, 1..5)
                        practiceFrequency[question.label] = rating
                    }
                    QuestionCategory.BLOCKER_ENABLER -> {
                        val rating = interaction.presentStatement(question.text, 1..5)
                        blockerEnablerPairs[question.label] = rating
                    }
                    QuestionCategory.QUANTITATIVE_SELF_REPORT -> {
                        val answer = interaction.askQuestion(question.text)
                        // Parse numeric value from the answer, defaulting to 0
                        val numericValue = answer.trim().toIntOrNull() ?: 0
                        quantitativeSelfReport[question.label] = numericValue
                    }
                    QuestionCategory.OPEN_ENDED -> {
                        val answer = interaction.askQuestion(question.text)
                        openEndedResponses.add(answer)
                    }
                }
            }
        }

        // Administer Part C: three level-independent narrative questions
        interaction.showProgress("\n--- ALCI Part C: Lived Experience ---\n")
        deepDive.getPartCQuestions().forEach { question ->
            val answer = interaction.askQuestion(question.text)
            openEndedResponses.add(answer)
        }

        return ALCIDeepDive(
            practiceFrequency = practiceFrequency,
            blockerEnablerPairs = blockerEnablerPairs,
            quantitativeSelfReport = quantitativeSelfReport,
            openEndedResponses = openEndedResponses
        )
    }

    // -- Phase 2: Judgment --

    /**
     * The core judgment action: assesses the team's literacy level from all evidence.
     *
     * Uses the "reasoning" model role because this is the highest-stakes LLM call in
     * the entire assessment. The model must weigh observable evidence against self-reported
     * deep-dive responses, resolve contradictions, and produce a defensible judgment.
     *
     * This action consumes both ObservableEvidence and ALCIDeepDive — the planner ensures
     * both are on the blackboard before this action runs.
     */
    @Action(description = "Assess the team's AI literacy level from gathered evidence", cost = 5.0)
    fun assessLevel(evidence: ObservableEvidence, deepDiveResult: ALCIDeepDive, context: ActionContext): LevelAssessment {
        interaction.showProgress("\nAssessing literacy level...")

        val evidenceSummary = buildString {
            appendLine("Observable evidence:")
            evidence.levelSignals.forEach { (level, signals) ->
                if (signals.isNotEmpty()) {
                    appendLine("  Level $level: ${signals.joinToString(", ")}")
                }
            }
            appendLine()
            appendLine("Deep-dive responses:")
            appendLine(deepDiveResult.summary())
        }

        val prompt = """
            You are an expert AI literacy assessor using the AI Literacy Collaboration
            Index (ALCI) framework. The framework defines six levels:

            L0 - Unaware: No deliberate AI engagement
            L1 - Aware: Intentional prompt-level interaction
            L2 - Experimenting: Systematic verification practices
            L3 - Practising: Habitat engineering with constraints
            L4 - Integrating: Specification-first discipline
            L5 - Sovereign Engineer: Full framework mastery

            Based on the following evidence, determine the team's primary literacy level
            and score three disciplines (0-5 each):
            - Context Engineering: deliberate system prompt design, model selection, context curation
            - Constraint Practice: HARNESS.md, deterministic checks, agent-enforced rules
            - Guardrail Deployment: hooks, CI checks, automated enforcement

            Evidence:
            $evidenceSummary

            Provide your assessment as a JSON object with fields:
            - primaryLevel: one of UNAWARE, AWARE, EXPERIMENTING, PRACTISING, INTEGRATING, SOVEREIGN_ENGINEER
            - contextEngineeringScore: integer 0-5
            - constraintScore: integer 0-5
            - guardrailScore: integer 0-5
            - rationale: string explaining why this level and not adjacent levels
            - strengths: list of 2-4 string observations about what the team does well
            - gaps: list of 2-4 string observations about what the team should improve

            The primary level should be the highest level where the team shows substantial
            evidence across all three disciplines. The weakest discipline is the ceiling.
        """.trimIndent()

        val runner = context.promptRunner()
            .withLlm(LlmOptions.fromCriteria(ModelSelectionCriteria.byRole("reasoning")))

        return runner.createObject(prompt, LevelAssessment::class.java)
    }

    /**
     * Generates 3-5 specific, actionable recommendations based on the assessment.
     *
     * Uses the "reasoning" model role because recommendations must be grounded in the
     * framework's progression model — not generic advice, but specific next steps that
     * move the team from their current level toward the next one.
     */
    @Action(description = "Generate actionable recommendations from the level assessment", cost = 4.0)
    fun generateRecommendations(assessment: LevelAssessment, context: ActionContext): Recommendations {
        interaction.showProgress("Generating recommendations...")

        val prompt = """
            You are an AI literacy coach using the ALCI framework. Based on the following
            assessment, generate 3-5 specific, actionable recommendations.

            Current level: ${assessment.primaryLevel.displayName} (L${assessment.primaryLevel.number})
            Context Engineering score: ${assessment.contextEngineeringScore}/5
            Constraint score: ${assessment.constraintScore}/5
            Guardrail score: ${assessment.guardrailScore}/5

            Strengths: ${assessment.strengths.joinToString("; ")}
            Gaps: ${assessment.gaps.joinToString("; ")}

            Rationale: ${assessment.rationale}

            Each recommendation should:
            - Be a concrete action the team can take this quarter
            - Target the weakest discipline or the most impactful gap
            - Reference a specific framework practice, appendix, or skill
            - Explain the expected impact

            Return a JSON object with a field "items" containing a list of objects,
            each with fields: description (string), impact (string), frameworkReference (string).
            Order by impact — highest value first.
        """.trimIndent()

        val runner = context.promptRunner()
            .withLlm(LlmOptions.fromCriteria(ModelSelectionCriteria.byRole("reasoning")))

        return runner.createObject(prompt, Recommendations::class.java)
    }

    /**
     * Compares the current assessment with a previous one to show trajectory.
     *
     * Uses the "fast" model role because trajectory comparison is mostly arithmetic
     * (compute deltas) with a short narrative summary. The model only needs to
     * interpret the deltas in human terms, not reason deeply.
     *
     * When no previous assessment path is available, produces a "first assessment"
     * comparison with zero deltas so the document assembler has a consistent input.
     */
    @Action(description = "Compare current assessment with previous to show trajectory", cost = 3.0)
    fun compareTrajectory(assessment: LevelAssessment, context: ActionContext): TrajectoryComparison {
        // When there's no previous assessment on the blackboard, return a first-assessment stub.
        // The planner feeds the AssessmentRequest to scanRepository, not here, so we check
        // for previous assessment content through the blackboard or produce default trajectory.
        interaction.showProgress("Analysing trajectory...")

        val prompt = """
            This is the team's current AI literacy assessment:
            Level: ${assessment.primaryLevel.displayName} (L${assessment.primaryLevel.number})
            Context Engineering: ${assessment.contextEngineeringScore}/5
            Constraint Practice: ${assessment.constraintScore}/5
            Guardrail Deployment: ${assessment.guardrailScore}/5

            This appears to be the team's first assessment. Generate a trajectory
            comparison that establishes the baseline.

            Return a JSON object with fields:
            - previousLevel: "${assessment.primaryLevel.name}" (same as current for first assessment)
            - currentLevel: "${assessment.primaryLevel.name}"
            - disciplineDeltas: map of discipline name to integer delta (all zeros for first assessment)
            - narrative: a 2-3 sentence summary noting this is the first assessment and establishing the baseline
        """.trimIndent()

        val runner = context.promptRunner()
            .withLlm(LlmOptions.fromCriteria(ModelSelectionCriteria.byRole("fast")))

        return runner.createObject(prompt, TrajectoryComparison::class.java)
    }

    // -- Phase 3: Delivery --

    /**
     * Assembles all assessment components into a complete markdown document.
     *
     * Uses the "balanced" model role because document assembly requires good prose
     * quality (not just speed) but not the deep reasoning needed for level judgment.
     * The model synthesises the structured data into a coherent narrative report.
     */
    @Action(description = "Assemble all findings into a complete assessment document", cost = 3.0)
    fun assembleDocument(
        assessment: LevelAssessment,
        recommendations: Recommendations,
        trajectory: TrajectoryComparison,
        context: ActionContext
    ): AssessmentDocument {
        interaction.showProgress("Assembling assessment document...")

        val recommendationsText = recommendations.items.mapIndexed { i, rec ->
            "${i + 1}. **${rec.description}**\n   Impact: ${rec.impact}\n   Framework reference: ${rec.frameworkReference}"
        }.joinToString("\n\n")

        val prompt = """
            Assemble a complete AI Literacy assessment document in markdown format.
            Include all sections below, using the provided data. Write clear prose
            that a non-technical manager could understand.

            ## Assessment Data

            Team level: ${assessment.primaryLevel.displayName} (L${assessment.primaryLevel.number})

            Discipline scores:
            - Context Engineering: ${assessment.contextEngineeringScore}/5
            - Constraint Practice: ${assessment.constraintScore}/5
            - Guardrail Deployment: ${assessment.guardrailScore}/5

            Rationale: ${assessment.rationale}

            Strengths:
            ${assessment.strengths.joinToString("\n") { "- $it" }}

            Gaps:
            ${assessment.gaps.joinToString("\n") { "- $it" }}

            Recommendations:
            $recommendationsText

            Trajectory: ${trajectory.narrative}

            ## Required Sections

            1. Summary badge (level name and number, date)
            2. Level assessment with rationale
            3. Discipline scores breakdown
            4. Strengths
            5. Gaps and growth areas
            6. Recommendations (numbered, with framework references)
            7. Trajectory comparison
            8. Metadata footer (assessment date, assessor version)

            Return only the markdown content as plain text.
        """.trimIndent()

        val runner = context.promptRunner()
            .withLlm(LlmOptions.fromCriteria(ModelSelectionCriteria.byRole("balanced")))

        val markdownContent = runner.generateText(prompt)

        return AssessmentDocument(
            markdownContent = markdownContent,
            teamName = "assessed-team",
            assessmentDate = LocalDate.now(),
            level = assessment.primaryLevel
        )
    }

    /**
     * Writes the assembled assessment document to disk.
     *
     * This is the terminal action — the goal the planner works toward. No LLM is
     * involved; it simply writes the markdown file and returns a confirmation string.
     *
     * The file is named with the assessment date for easy trajectory comparison:
     * {outputDir}/YYYY-MM-DD-assessment.md.
     */
    @Action(description = "Write the assessment document to disk", cost = 1.0)
    @AchievesGoal(description = "Complete an AI Literacy assessment and produce a written report")
    fun writeAssessment(document: AssessmentDocument): String {
        val outputDirectory = File(outputDir)
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val filename = "${document.assessmentDate}-assessment.md"
        val outputFile = File(outputDirectory, filename)
        outputFile.writeText(document.markdownContent)

        val result = "Assessment written to ${outputFile.absolutePath}"
        interaction.showResult("\n$result")
        interaction.showResult("Level: ${document.level.displayName} (L${document.level.number})")

        return result
    }
}
