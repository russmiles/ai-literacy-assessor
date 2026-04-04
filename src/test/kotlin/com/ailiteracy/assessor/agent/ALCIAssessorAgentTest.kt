/**
 * Tests for the deterministic actions of ALCIAssessorAgent.
 *
 * The agent has ten actions, but only four are testable without an LLM: scanRepository,
 * collectResponses, administerPlacement, and administerDeepDive. The LLM-driven actions
 * (generateQuestions, assessLevel, generateRecommendations, compareTrajectory,
 * assembleDocument) require FakeOperationContext with canned LLM responses — those
 * belong in a separate integration test file.
 *
 * These tests verify the agent's deterministic logic: delegation to the scanner,
 * delegation to the placement instrument, and user interaction wiring. They use a
 * fake UserInteractionPort that returns predetermined answers, isolating the agent
 * from real I/O.
 */
package com.ailiteracy.assessor.agent

import com.ailiteracy.assessor.alci.DeepDiveInstrument
import com.ailiteracy.assessor.alci.PlacementInstrument
import com.ailiteracy.assessor.domain.AssessmentRequest
import com.ailiteracy.assessor.domain.ClarifyingQuestions
import com.ailiteracy.assessor.domain.LiteracyLevel
import com.ailiteracy.assessor.domain.ObservableEvidence
import com.ailiteracy.assessor.interaction.UserInteractionPort
import com.ailiteracy.assessor.scanner.RepositoryScanner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * A fake interaction port that returns predetermined responses in sequence.
 *
 * Each call to [askQuestion] pops the next answer from [answers]. Each call to
 * [presentStatement] returns the next value from [ratings]. This lets tests
 * control exactly what the agent receives without touching real I/O.
 */
class FakeUserInteraction(
    private val answers: MutableList<String> = mutableListOf(),
    private val ratings: MutableList<Int> = mutableListOf()
) : UserInteractionPort {

    val questionsAsked = mutableListOf<String>()
    val statementsPresented = mutableListOf<String>()
    val progressMessages = mutableListOf<String>()
    val resultMessages = mutableListOf<String>()

    override fun askQuestion(question: String): String {
        questionsAsked.add(question)
        return if (answers.isNotEmpty()) answers.removeAt(0) else ""
    }

    override fun presentStatement(statement: String, scale: IntRange): Int {
        statementsPresented.add(statement)
        return if (ratings.isNotEmpty()) ratings.removeAt(0) else scale.first
    }

    override fun showProgress(message: String) {
        progressMessages.add(message)
    }

    override fun showResult(message: String) {
        resultMessages.add(message)
    }
}

class ALCIAssessorAgentTest {

    private val scanner = RepositoryScanner()
    private val placement = PlacementInstrument()
    private val deepDive = DeepDiveInstrument()

    // --- scanRepository ---

    @Test
    fun `scanRepository returns empty evidence when no repo path provided`() {
        val interaction = FakeUserInteraction()
        val agent = ALCIAssessorAgent(scanner, placement, deepDive, interaction)
        val request = AssessmentRequest(teamName = "Test Team", repoPath = null)

        val evidence = agent.scanRepository(request)

        assertEquals(0, evidence.ciWorkflowCount)
        assertEquals(false, evidence.hasClaudeMd)
        assertEquals(0, evidence.skillCount)
    }

    @Test
    fun `scanRepository delegates to scanner when repo path provided`(@TempDir dir: Path) {
        // Plant a CLAUDE.md so the scanner finds something
        File(dir.toFile(), "CLAUDE.md").writeText("# Conventions")
        val interaction = FakeUserInteraction()
        val agent = ALCIAssessorAgent(scanner, placement, deepDive, interaction)
        val request = AssessmentRequest(teamName = "Test Team", repoPath = dir.toString())

        val evidence = agent.scanRepository(request)

        assertTrue(evidence.hasClaudeMd)
    }

    // --- collectResponses ---

    @Test
    fun `collectResponses collects answers for each question`() {
        val interaction = FakeUserInteraction(
            answers = mutableListOf("Answer one", "Answer two", "Answer three")
        )
        val agent = ALCIAssessorAgent(scanner, placement, deepDive, interaction)
        val questions = ClarifyingQuestions(
            questions = listOf("Q1?", "Q2?", "Q3?")
        )

        val responses = agent.collectResponses(questions)

        assertEquals(3, responses.responses.size)
        assertEquals("Answer one", responses.responses["Q1?"])
        assertEquals("Answer two", responses.responses["Q2?"])
        assertEquals("Answer three", responses.responses["Q3?"])
    }

    // --- administerPlacement ---

    @Test
    fun `administerPlacement computes correct level from scores`() {
        // Rate L3 statements highest (5,5), everything else low (1,1).
        // The placement instrument has 10 statements ordered L0..L4, two per level.
        // So positions 0-1 are L0, 2-3 are L1, 4-5 are L2, 6-7 are L3, 8-9 are L4.
        val ratings = mutableListOf(1, 1, 1, 1, 1, 1, 5, 5, 1, 1)
        val interaction = FakeUserInteraction(ratings = ratings)
        val agent = ALCIAssessorAgent(scanner, placement, deepDive, interaction)

        // administerPlacement takes ClarifyingResponses as its prerequisite
        val responses = com.ailiteracy.assessor.domain.ClarifyingResponses(
            responses = mapOf("Q1?" to "Answer")
        )

        val result = agent.administerPlacement(responses)

        assertEquals(LiteracyLevel.PRACTISING, result.primaryLevel)
        // Adjacent level should be L4 (one above primary)
        assertEquals(LiteracyLevel.INTEGRATING, result.adjacentLevel)
    }

    @Test
    fun `administerPlacement breaks ties in favour of lower level`() {
        // Rate both L1 and L2 equally high (5,5), everything else low (1,1).
        val ratings = mutableListOf(1, 1, 5, 5, 5, 5, 1, 1, 1, 1)
        val interaction = FakeUserInteraction(ratings = ratings)
        val agent = ALCIAssessorAgent(scanner, placement, deepDive, interaction)

        val responses = com.ailiteracy.assessor.domain.ClarifyingResponses(
            responses = mapOf("Q1?" to "Answer")
        )

        val result = agent.administerPlacement(responses)

        // Tie between L1 and L2 should resolve to L1 (lower wins)
        assertEquals(LiteracyLevel.AWARE, result.primaryLevel)
    }
}
