/**
 * AssessCommand bridges the Spring Shell CLI and the ALCI assessment agent.
 * It exists because the Embabel shell provides generic commands (execute, chat)
 * but the README promises a domain-specific `assess` command with typed flags
 * for team name and repository path.
 *
 * The command constructs an [AssessmentRequest], looks up the deployed
 * ALCIAssessorAgent, and hands both to Embabel's [Autonomy] to plan and
 * execute the full assessment workflow. All user interaction during the
 * assessment flows through the [UserInteractionPort], not through this command.
 *
 * This file does not contain assessment logic — it is purely a CLI adapter.
 */
package com.ailiteracy.assessor.shell

import com.ailiteracy.assessor.domain.AssessmentRequest
import com.embabel.agent.api.common.autonomy.Autonomy
import com.embabel.agent.core.ProcessOptions
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

@ShellComponent
class AssessCommand(
    private val autonomy: Autonomy
) {

    /**
     * Starts an ALCI assessment for the given team. The agent is resolved by
     * name from the platform's deployed agents rather than injected directly,
     * because Embabel deploys agents as runtime objects, not Spring beans.
     */
    @ShellMethod(
        key = ["assess"],
        value = "Run an AI Literacy Collaboration Index assessment"
    )
    fun assess(
        @ShellOption("--team", help = "Team name for the assessment") team: String,
        @ShellOption("--repo", help = "Path to the repository to scan", defaultValue = ShellOption.NULL) repo: String?,
        @ShellOption("--previous", help = "Path to a previous assessment for trajectory comparison", defaultValue = ShellOption.NULL) previous: String?
    ): String {
        val request = AssessmentRequest(
            teamName = team,
            repoPath = repo,
            previousAssessmentPath = previous
        )

        val agent = autonomy.agentPlatform.agents()
            .firstOrNull { it.name == "ALCIAssessorAgent" }
            ?: return "Error: ALCIAssessorAgent is not deployed."

        return try {
            val execution = autonomy.runAgent(request, ProcessOptions(), agent)
            execution.output?.toString() ?: "Assessment completed."
        } catch (e: Exception) {
            "Assessment failed: ${e.message}"
        }
    }
}
