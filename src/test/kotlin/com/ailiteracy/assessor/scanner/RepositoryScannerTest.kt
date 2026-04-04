/**
 * Tests for RepositoryScanner using JUnit 5 @TempDir fixtures.
 *
 * Each test constructs a minimal synthetic repository tree in a temporary
 * directory, invokes the scanner, and asserts that the returned
 * ObservableEvidence reflects what was planted. This approach lets us verify
 * each signal in isolation without touching a real repo on disk.
 *
 * The tests are ordered from simplest (single-file detection) to most
 * complex (HARNESS.md parsing), mirroring the scanner's internal logic
 * order so that a failure points immediately to which layer broke.
 */
package com.ailiteracy.assessor.scanner

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class RepositoryScannerTest {

    private val scanner = RepositoryScanner()

    @Test
    fun `scan detects CLAUDE md when present`(@TempDir dir: Path) {
        File(dir.toFile(), "CLAUDE.md").writeText("# Conventions")
        val evidence = scanner.scan(dir.toString())
        assertTrue(evidence.hasClaudeMd)
    }

    @Test
    fun `scan reports false when CLAUDE md absent`(@TempDir dir: Path) {
        val evidence = scanner.scan(dir.toString())
        assertFalse(evidence.hasClaudeMd)
    }

    @Test
    fun `scan counts skills in claude skills directory`(@TempDir dir: Path) {
        // Each subdirectory of .claude/skills that contains a SKILL.md counts as one skill
        val skillDir = File(dir.toFile(), ".claude/skills/literate-programming")
        skillDir.mkdirs()
        File(skillDir, "SKILL.md").writeText("---\nname: test\n---")
        val evidence = scanner.scan(dir.toString())
        assertEquals(1, evidence.skillCount)
    }

    @Test
    fun `scan detects CI workflows`(@TempDir dir: Path) {
        val workflowDir = File(dir.toFile(), ".github/workflows")
        workflowDir.mkdirs()
        File(workflowDir, "test.yml").writeText("name: Test")
        val evidence = scanner.scan(dir.toString())
        assertEquals(1, evidence.ciWorkflowCount)
    }

    @Test
    fun `scan parses HARNESS md constraint count`(@TempDir dir: Path) {
        // Three constraint blocks — one deterministic, one agent, one unverified.
        // deterministicConstraintCount + agentConstraintCount should be 2 (enforced tiers).
        // unverifiedConstraintCount should be 1.
        File(dir.toFile(), "HARNESS.md").writeText(
            """
            ## Constraints
            ### Rule one
            - **Enforcement**: deterministic
            ### Rule two
            - **Enforcement**: agent
            ### Rule three
            - **Enforcement**: unverified
            """.trimIndent()
        )
        val evidence = scanner.scan(dir.toString())
        assertEquals(1, evidence.deterministicConstraintCount)
        assertEquals(1, evidence.agentConstraintCount)
        assertEquals(1, evidence.unverifiedConstraintCount)
    }
}
