/**
 * UserInteractionPort abstracts all user-facing I/O so the assessment agent
 * can run unchanged across both delivery modes: interactive terminal (CLI) and
 * web chat (SSE stream).
 *
 * The agent knows nothing about whether it is talking to a shell or a browser.
 * Every question it asks, every statement it presents for rating, every progress
 * update it emits, and every result it shows flows through this interface.
 * The concrete implementation wired by Spring's active profile handles the
 * channel-specific details.
 *
 * This separation is the architectural commitment that makes multi-frontend
 * support possible without forking agent logic. The four methods map onto
 * the four interaction modes the assessment protocol requires: open-ended
 * questioning, Likert-scale rating, progress signalling, and result display.
 *
 * This file contains only the interface contract. ShellInteraction provides
 * the terminal implementation; WebInteraction (in a separate file) provides
 * the SSE implementation for web chat.
 */
package com.ailiteracy.assessor.interaction

/**
 * The I/O contract between the assessment agent and the delivery channel.
 *
 * Implementations must be thread-safe if the agent runs asynchronously. The
 * current implementations are synchronous (one active assessment at a time),
 * but the interface does not guarantee blocking behaviour — callers should
 * not assume a response is available before this method returns.
 */
interface UserInteractionPort {

    /**
     * Presents an open-ended question and returns the user's response as a
     * string.
     *
     * The question text should be complete and self-contained — do not rely
     * on prior conversational context being visible to the user. The returned
     * string is trimmed of leading and trailing whitespace by convention;
     * implementations should enforce this.
     */
    fun askQuestion(question: String): String

    /**
     * Presents a Likert-style statement and returns the user's numeric rating.
     *
     * [scale] defines the valid response range. The assessment protocol uses
     * 1..5 for most items, but some quantitative self-report items use wider
     * ranges. Implementations must validate that the returned Int falls within
     * [scale] and re-prompt if it does not — the contract is that the returned
     * value is always a valid member of [scale].
     */
    fun presentStatement(statement: String, scale: IntRange): Int

    /**
     * Displays a progress message to the user without expecting a response.
     *
     * Used to signal phase transitions, acknowledge responses, and orient the
     * user during longer sections of the instrument. Implementations may render
     * this differently from a question — e.g. dimmed text in the terminal,
     * a status line in the web UI.
     */
    fun showProgress(message: String)

    /**
     * Displays a final result or summary to the user without expecting a
     * response.
     *
     * Used for assessment outcomes, level announcements, and recommendation
     * summaries. Implementations should render this with higher visual weight
     * than progress messages — it is the payoff the user has been working
     * toward.
     */
    fun showResult(message: String)
}
