/**
 * ShellInteraction is the terminal implementation of UserInteractionPort,
 * activated when the application runs without the "web" profile.
 *
 * It writes prompts and messages to standard output and reads responses from
 * standard input via a single shared Scanner. The Scanner is constructed once
 * at startup and held for the lifetime of the application — creating a new
 * Scanner per call would risk losing buffered input between calls, because the
 * underlying InputStream would be partially consumed.
 *
 * The assessment protocol is synchronous: one question at a time, one user
 * response at a time. ShellInteraction does not need to handle concurrent
 * access, so no locking is applied. If the protocol ever needs parallelism,
 * that would require a different implementation profile.
 *
 * This class validates scale bounds on presentStatement: if the user types a
 * value outside the allowed range, it re-prompts rather than crashing or
 * silently accepting bad data. The loop is tight because assessment ratings
 * are short interactions — a user who mistypes "6" when the scale is 1–5
 * expects an immediate re-prompt, not an error screen.
 *
 * This file does not contain the WebInteraction implementation. That lives in
 * WebInteraction.kt and is activated by @Profile("web").
 */
package com.ailiteracy.assessor.interaction

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.Scanner

/**
 * Terminal implementation of UserInteractionPort, active in all profiles
 * except "web".
 *
 * Uses a single Scanner over System.in to read responses. Prints prompts
 * and messages to System.out directly — no logging framework, because these
 * are user-visible conversational turns, not application log events.
 */
@Component
@Profile("!web")
class ShellInteraction : UserInteractionPort {

    // A single Scanner held for the application lifetime. Sharing one Scanner
    // across calls is intentional — if we created a new Scanner per call, the
    // JVM's BufferedReader backing System.in could lose data between reads.
    private val scanner = Scanner(System.`in`)

    /**
     * Prints the question, reads a line, and returns it trimmed.
     *
     * The blank line before the question and the "> " prompt are visual
     * conventions that help the user distinguish questions from progress
     * messages in a scrolling terminal session.
     */
    override fun askQuestion(question: String): String {
        println()
        println(question)
        print("> ")
        return scanner.nextLine().trim()
    }

    /**
     * Prints the statement with scale bounds, then loops until the user
     * provides a valid integer within the scale.
     *
     * The scale is displayed as "[min–max]" rather than listing every value,
     * because the assessment uses 1–5 throughout and listing each value would
     * add visual clutter without helping comprehension. The re-prompt message
     * is explicit about what was wrong, not just a generic "try again".
     */
    override fun presentStatement(statement: String, scale: IntRange): Int {
        println()
        println(statement)
        println("[${scale.first}–${scale.last}]")

        while (true) {
            print("> ")
            val input = scanner.nextLine().trim()
            val value = input.toIntOrNull()
            if (value != null && value in scale) {
                return value
            }
            // The user typed something outside the valid range or non-numeric.
            // Re-prompt immediately — don't swallow the error silently.
            println("Please enter a number between ${scale.first} and ${scale.last}.")
        }
    }

    /**
     * Prints a progress message prefixed with "→ " to distinguish it visually
     * from questions and results in the terminal transcript.
     */
    override fun showProgress(message: String) {
        println()
        println("→ $message")
    }

    /**
     * Prints a result message surrounded by blank lines and prefixed with
     * "★ " to give it visual weight appropriate to the assessment payoff.
     */
    override fun showResult(message: String) {
        println()
        println("★ $message")
        println()
    }
}
