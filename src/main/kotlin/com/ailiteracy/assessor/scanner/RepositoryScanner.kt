/**
 * Scans a local repository for observable evidence of AI literacy practices.
 * Every check is deterministic — file existence, directory traversal, regex
 * matching against known markers. No LLM is involved.
 *
 * The scanner's job is to answer one question: "what artefacts indicate that
 * someone has deliberately engineered this repository as an AI collaboration
 * habitat?" It translates the framework's sixteen level signals into concrete
 * filesystem checks and produces an ObservableEvidence value object that the
 * assessment agent can reason about without re-reading the disk.
 *
 * The scanner deliberately does not interpret what it finds. It counts CI
 * workflows but does not decide whether four workflows is good or bad. It
 * counts HARNESS.md constraint tiers but does not score them. Interpretation
 * belongs to the assessment agent's LLM-driven actions, not here.
 *
 * When a repo path is not available (no path provided, or the path does not
 * exist), the scanner returns an empty ObservableEvidence with all defaults.
 * The assessment agent then falls back to conversational evidence gathering.
 * This file does not handle that fallback — it simply handles the null/missing
 * directory case gracefully by treating absent directories as empty ones.
 */
package com.ailiteracy.assessor.scanner

import com.ailiteracy.assessor.domain.ObservableEvidence
import org.springframework.stereotype.Component
import java.io.File

/**
 * Spring component that performs deterministic repository scanning.
 *
 * Instantiated by Spring and injected into the assessment agent. The single
 * public method [scan] is the entire surface area — callers pass a path string
 * and receive a fully-populated ObservableEvidence. The class holds no state
 * between calls, making it safe to share across concurrent assessments.
 */
@Component
class RepositoryScanner {

    /**
     * Scans the repository at [repoPath] and returns a snapshot of observable
     * evidence. The scan is structured in three passes — L2 signals (CI and
     * engineering hygiene), L3 signals (AI collaboration habitat), and L4
     * signals (spec-first practice) — mirroring the framework's level taxonomy
     * so that the evidence map can be read directly by the assessment agent.
     *
     * Missing directories produce empty collections rather than null values.
     * Missing files produce false booleans or zero counts. The caller never
     * needs to null-check the returned object.
     */
    fun scan(repoPath: String): ObservableEvidence {
        val root = File(repoPath)
        val levelSignals = mutableMapOf<Int, MutableList<String>>()
        (0..5).forEach { levelSignals[it] = mutableListOf() }

        // --- L2: Basic engineering hygiene visible in CI configuration ---
        //
        // CI workflow files are the most reliable L2 signal because their
        // presence on disk proves they were committed, not just run locally.
        // We count only .yml/.yaml files to avoid picking up README drafts.
        val workflowDir = root.resolve(".github/workflows")
        val workflowNames = workflowDir
            .listFiles { f -> f.extension == "yml" || f.extension == "yaml" }
            ?.map { it.name } ?: emptyList()
        val ciWorkflowCount = workflowNames.size
        if (ciWorkflowCount > 0) levelSignals[2]!!.add("CI workflows: $ciWorkflowCount")

        // Coverage, vuln scanning, and mutation testing are detected by
        // grepping workflow content. A name match is insufficient — a workflow
        // named "build.yml" can contain coverage steps.
        val workflowTexts = workflowNames.map { name ->
            workflowDir.resolve(name).readText()
        }

        val hasTestCoverage = workflowTexts.any { text ->
            text.contains(Regex("coverage|coverprofile|jacoco|pytest-cov|coverlet", RegexOption.IGNORE_CASE))
        }
        if (hasTestCoverage) levelSignals[2]!!.add("Test coverage enforcement")

        val hasVulnerabilityScanning = workflowTexts.any { text ->
            text.contains(Regex("govulncheck|docker scout|owasp|dependency-check", RegexOption.IGNORE_CASE))
        }
        if (hasVulnerabilityScanning) levelSignals[2]!!.add("Vulnerability scanning")

        val hasMutationTesting = workflowTexts.any { text ->
            text.contains(Regex("mutesting|pitest|mutmut|stryker", RegexOption.IGNORE_CASE))
        }
        if (hasMutationTesting) levelSignals[2]!!.add("Mutation testing")

        // --- L3: Deliberate AI collaboration habitat ---
        //
        // CLAUDE.md is the single most important L3 signal. Its presence means
        // someone took time to write down how the AI should behave in this repo.
        val hasClaudeMd = root.resolve("CLAUDE.md").exists()
        if (hasClaudeMd) levelSignals[3]!!.add("CLAUDE.md")

        val hasAgentsMd = root.resolve("AGENTS.md").exists()
        if (hasAgentsMd) levelSignals[3]!!.add("AGENTS.md")

        val hasModelRoutingMd = root.resolve("MODEL_ROUTING.md").exists()
        if (hasModelRoutingMd) levelSignals[3]!!.add("MODEL_ROUTING.md")

        val hasReflectionLog = root.resolve("REFLECTION_LOG.md").exists()
        val reflectionEntryCount = if (hasReflectionLog) {
            root.resolve("REFLECTION_LOG.md").readText()
                .lines()
                .count { it.startsWith("## ") && it.contains(Regex("\\d{4}-\\d{2}-\\d{2}")) }
        } else 0
        if (hasReflectionLog) levelSignals[3]!!.add("REFLECTION_LOG.md ($reflectionEntryCount entries)")

        // HARNESS.md is present when the team has deliberately catalogued their
        // enforcement constraints. We parse three tiers — deterministic (tooling
        // enforces), agent (LLM hook enforces), unverified (honour system) —
        // because the ratio of deterministic to unverified is itself a signal.
        val hasHarnessMd = root.resolve("HARNESS.md").exists()
        var deterministicConstraintCount = 0
        var agentConstraintCount = 0
        var unverifiedConstraintCount = 0
        var agentEntryCount = 0

        if (hasHarnessMd) {
            levelSignals[3]!!.add("HARNESS.md")
            val harnessText = root.resolve("HARNESS.md").readText()
            // Split on **Enforcement**: markers and classify the tier that follows.
            // drop(1) skips the text before the first marker.
            harnessText.split("**Enforcement**:").drop(1).forEach { segment ->
                val tier = segment.trimStart()
                when {
                    tier.startsWith("deterministic") -> deterministicConstraintCount++
                    tier.startsWith("agent") -> agentConstraintCount++
                    tier.startsWith("unverified") -> unverifiedConstraintCount++
                }
            }
            // Count AGENTS.md entries as a proxy for agent-authored conventions
            if (hasAgentsMd) {
                agentEntryCount = root.resolve("AGENTS.md").readText()
                    .lines()
                    .count { it.startsWith("## ") }
            }
        }

        // Skills are directories under .claude/skills that contain a SKILL.md.
        // Counting directories rather than files prevents partial skill drafts
        // from inflating the count.
        val skillCount = root.resolve(".claude/skills")
            .listFiles { f -> f.isDirectory }
            ?.count { File(it, "SKILL.md").exists() } ?: 0
        if (skillCount > 0) levelSignals[3]!!.add("Skills: $skillCount")

        // Agents and commands follow the same directory-based counting pattern.
        val agentCount = (root.resolve(".claude/agents")
            .listFiles { f -> f.extension == "md" }?.size ?: 0) +
            (root.resolve("agents")
                .listFiles { f -> f.name.endsWith(".agent.md") }?.size ?: 0)
        if (agentCount > 0) levelSignals[3]!!.add("Agents: $agentCount")

        val commandCount = root.resolve(".claude/commands")
            .listFiles { f -> f.extension == "md" }?.size ?: 0
        if (commandCount > 0) levelSignals[3]!!.add("Commands: $commandCount")

        // Hooks can live in multiple locations depending on how the Claude Code
        // harness was configured. Any of these paths counts as "hooks present".
        val hasHooks = root.resolve("hooks.json").exists() ||
            root.resolve(".claude/settings.json").exists() ||
            root.resolve("hooks/hooks.json").exists()
        if (hasHooks) levelSignals[3]!!.add("Hooks configured")

        // --- L4: Spec-first development practice ---
        //
        // We look for specs/<app>/spec.md rather than any Markdown file under
        // specs/, to avoid counting README stubs or plan drafts. A spec.md in
        // the expected location means someone followed the spec-first workflow.
        val specDirs = root.resolve("specs").listFiles { f -> f.isDirectory } ?: emptyArray()
        val specCount = specDirs.count { File(it, "spec.md").exists() }
        val planCount = specDirs.count { File(it, "plan.md").exists() }
        if (specCount > 0) levelSignals[4]!!.add("Specifications: $specCount")
        if (planCount > 0) levelSignals[4]!!.add("Plans: $planCount")

        // --- L3+: Observability practice ---
        val observabilitySnapshotCount = root.resolve("observability/snapshots")
            .listFiles { f -> f.name.endsWith("-snapshot.md") }?.size ?: 0
        if (observabilitySnapshotCount > 0) levelSignals[3]!!.add("Observability snapshots: $observabilitySnapshotCount")

        return ObservableEvidence(
            ciWorkflowCount = ciWorkflowCount,
            hasTestCoverage = hasTestCoverage,
            hasVulnerabilityScanning = hasVulnerabilityScanning,
            hasMutationTesting = hasMutationTesting,
            hasClaudeMd = hasClaudeMd,
            hasHarnessMd = hasHarnessMd,
            hasAgentsMd = hasAgentsMd,
            hasModelRoutingMd = hasModelRoutingMd,
            hasReflectionLog = hasReflectionLog,
            skillCount = skillCount,
            agentCount = agentCount,
            commandCount = commandCount,
            hasHooks = hasHooks,
            deterministicConstraintCount = deterministicConstraintCount,
            agentConstraintCount = agentConstraintCount,
            unverifiedConstraintCount = unverifiedConstraintCount,
            reflectionEntryCount = reflectionEntryCount,
            agentEntryCount = agentEntryCount,
            specCount = specCount,
            planCount = planCount,
            observabilitySnapshotCount = observabilitySnapshotCount,
            levelSignals = levelSignals
        )
    }
}
