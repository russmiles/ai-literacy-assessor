/**
 * WebInteraction is the SSE-backed implementation of UserInteractionPort,
 * activated when the application runs with the "web" Spring profile.
 *
 * The fundamental challenge this class solves is the impedance mismatch between
 * the agent's blocking, synchronous interaction model (ask a question, wait for
 * an answer) and the browser's event-driven, non-blocking HTTP model (stream
 * messages out via SSE, receive responses via separate POST requests). Blocking
 * queues bridge the two: the agent blocks on [incoming] waiting for the browser
 * to POST a response, while [outgoing] accumulates messages that the SSE endpoint
 * drains at whatever pace the browser can consume them.
 *
 * The design intentionally supports only one active assessment at a time. Adding
 * session isolation would require a map from session ID to queue pair, which adds
 * complexity that is not needed for the current single-user deployment model.
 * If multi-session support is needed later, the queue pair should be extracted
 * to a session-scoped bean.
 *
 * This class does not own the HTTP layer. ChatController reads from and writes
 * to the queues; WebInteraction's job is to be the agent-facing half of the bridge.
 *
 * This file does not contain ShellInteraction. That lives in ShellInteraction.kt
 * and is activated when the "web" profile is NOT active.
 */
package com.ailiteracy.assessor.interaction

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Web implementation of UserInteractionPort, active under the "web" profile.
 *
 * Two queues carry traffic in opposite directions. [outgoing] accumulates
 * messages the agent wants to show the user (questions, progress notes, results).
 * [incoming] carries the user's responses back to the blocking agent thread.
 * Both queues are thread-safe by construction — the agent thread and the HTTP
 * request threads access them from different goroutines.
 */
@Component
@Profile("web")
class WebInteraction : UserInteractionPort {

    // Outgoing messages flow from the agent to the browser via SSE. CopyOnWriteArrayList
    // is used for the accumulator because the SSE endpoint may iterate it at any time;
    // LinkedBlockingQueue serves as the signal channel so the SSE endpoint can block-poll
    // rather than busy-wait.
    val outgoingQueue: LinkedBlockingQueue<String> = LinkedBlockingQueue()

    // Incoming responses flow from the browser to the blocking agent thread. A single
    // element capacity would be cleaner but an unbounded queue lets us handle delayed
    // POSTs without dropping data if the agent somehow re-reads.
    val incomingQueue: LinkedBlockingQueue<String> = LinkedBlockingQueue()

    /**
     * Sends the question to the browser and blocks until the user responds.
     *
     * The "QUESTION:" prefix lets the front-end style this message differently
     * from progress notes — the UI uses it to enable the input field and indicate
     * that a response is expected.
     */
    override fun askQuestion(question: String): String {
        outgoingQueue.put("QUESTION:$question")
        // Block here on the agent thread until the browser POSTs a response.
        // This is the synchronisation point that makes the web UI feel conversational
        // despite the underlying request-response nature of HTTP.
        return incomingQueue.take().trim()
    }

    /**
     * Sends the statement with scale bounds and blocks until the browser returns
     * a valid integer within the scale.
     *
     * The "SCALE:" prefix signals the front-end to render a numeric input with
     * min/max bounds. Validation is performed here (not in the controller) because
     * the scale semantics live with the interaction contract, not the HTTP layer.
     * Invalid responses are rejected immediately and the browser is prompted again.
     */
    override fun presentStatement(statement: String, scale: IntRange): Int {
        outgoingQueue.put("SCALE:$statement|${scale.first}|${scale.last}")

        while (true) {
            val response = incomingQueue.take().trim()
            val value = response.toIntOrNull()
            if (value != null && value in scale) {
                return value
            }
            // The user sent something outside the valid range. Requeue a correction
            // prompt — this echoes the ShellInteraction behaviour so the agent's
            // error handling contract holds across both delivery channels.
            outgoingQueue.put("PROGRESS:Please enter a number between ${scale.first} and ${scale.last}.")
            outgoingQueue.put("SCALE:$statement|${scale.first}|${scale.last}")
        }
    }

    /**
     * Sends a progress message to the browser without expecting a response.
     *
     * The "PROGRESS:" prefix lets the front-end render this with lower visual
     * weight — dimmed or italicised — to distinguish it from questions and results.
     */
    override fun showProgress(message: String) {
        outgoingQueue.put("PROGRESS:$message")
    }

    /**
     * Sends a result message to the browser without expecting a response.
     *
     * The "RESULT:" prefix signals the front-end to render this with the highest
     * visual weight — it is the payoff the user has been working toward, and
     * should be visually distinct from both questions and progress notes.
     */
    override fun showResult(message: String) {
        outgoingQueue.put("RESULT:$message")
    }
}
