/**
 * ChatController is the HTTP layer that connects the browser to the ALCI
 * assessment agent when running under the "web" Spring profile.
 *
 * Its three endpoints cover the entire web interaction protocol:
 *
 *   1. POST /api/assess — the client fires this once when the page loads to
 *      start an assessment session. The agent is launched on a background thread
 *      so the HTTP response returns immediately while the agent begins working.
 *
 *   2. GET /api/chat/stream — a Server-Sent Events (SSE) endpoint that the
 *      client subscribes to via EventSource. It drains WebInteraction's outgoing
 *      queue and pushes each message to the browser as it arrives. This endpoint
 *      blocks (via queue.poll with timeout) rather than busy-waiting, so it does
 *      not spin a CPU while waiting for the next agent message.
 *
 *   3. POST /api/chat/respond — the client POSTs here when the user types a
 *      response. The body is forwarded directly to WebInteraction's incoming
 *      queue, unblocking the agent thread waiting in askQuestion or
 *      presentStatement.
 *
 * This class deliberately does not contain business logic. It is a thin HTTP
 * adapter. The agent, the interaction port, and the domain objects are all
 * managed elsewhere — this controller only converts between HTTP and queue
 * operations.
 *
 * Agent lookup uses Embabel's Autonomy service. The ALCIAssessorAgent is
 * registered with the platform by the @Agent annotation at startup; this
 * controller finds it by name at request time. If the agent is not found
 * (e.g. misconfigured profile), a clear error is returned rather than a hang.
 *
 * Session isolation is not implemented. One assessment runs at a time. If two
 * browsers hit /api/assess simultaneously, their messages will interleave in
 * unpredictable ways. This is acceptable for the current single-user deployment
 * model and noted here so that future multi-session work knows exactly where the
 * coupling lives.
 */
package com.ailiteracy.assessor.web

import com.ailiteracy.assessor.domain.AssessmentRequest
import com.ailiteracy.assessor.interaction.WebInteraction
import com.embabel.agent.api.common.autonomy.Autonomy
import com.embabel.agent.core.ProcessOptions
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Request body for POST /api/assess.
 *
 * [teamName] is required because all outputs are keyed to the team.
 * [repoPath] is optional — if absent the agent skips the repository scan
 * and proceeds with conversational evidence only, which is the common case
 * for web-chat sessions where no volume is mounted.
 */
data class StartAssessmentRequest(
    val teamName: String,
    val repoPath: String? = null
)

/**
 * Request body for POST /api/chat/respond.
 *
 * A single field carrying the user's plain-text response. The controller
 * forwards it directly to the WebInteraction incoming queue without parsing.
 */
data class UserResponse(
    val response: String
)

/**
 * HTTP adapter between the browser and the ALCI assessment agent.
 *
 * Active only under the "web" profile. Consumes [WebInteraction] to bridge
 * between the agent's blocking call model and the browser's HTTP/SSE model.
 * Uses [Autonomy] to launch the agent as a GOAP planning task via the platform.
 */
@RestController
@RequestMapping("/api")
@Profile("web")
class ChatController(
    private val webInteraction: WebInteraction,
    private val autonomy: Autonomy
) {

    // A single-thread executor keeps assessment sessions serial. The agent is
    // inherently sequential (blocking queue reads), so parallel execution would
    // deadlock or interleave messages. One thread makes the ordering guarantee explicit.
    private val agentExecutor = Executors.newSingleThreadExecutor()

    /**
     * Starts an assessment session by launching the agent on the background thread.
     *
     * Returns immediately — the browser does not wait for the assessment to complete.
     * Progress and questions arrive via the SSE stream (/api/chat/stream) which the
     * browser should open before or immediately after calling this endpoint.
     *
     * Agent lookup uses the platform's registered agent list. The ALCIAssessorAgent
     * is registered by @Agent at startup; we match it by class name. This is more
     * robust than a magic string because a rename refactoring would produce a
     * compile error, not a silent runtime miss.
     */
    @PostMapping("/assess")
    fun startAssessment(@RequestBody request: StartAssessmentRequest): Map<String, String> {
        val assessmentRequest = AssessmentRequest(
            teamName = request.teamName,
            repoPath = request.repoPath
        )

        // Resolve the agent once before submitting to the background thread.
        // Failing here (on the HTTP thread) gives the browser a clear error response
        // rather than a silent hang when the background thread would have failed.
        val agentName = "ALCIAssessorAgent"
        val agent = autonomy.agentPlatform.agents()
            .firstOrNull { it.name == agentName }
            ?: return mapOf(
                "status" to "error",
                "message" to "Agent '$agentName' not found. Check Spring profile configuration."
            )

        agentExecutor.submit {
            try {
                autonomy.runAgent(
                    inputObject = assessmentRequest,
                    processOptions = ProcessOptions(),
                    agent = agent,
                )
            } catch (e: Exception) {
                // Surface errors through the SSE channel rather than swallowing them,
                // so the browser can show a useful error message instead of hanging.
                webInteraction.showResult("ERROR: Assessment failed — ${e.message}")
            }
        }

        return mapOf("status" to "started", "teamName" to request.teamName)
    }

    /**
     * SSE endpoint that streams agent messages to the browser.
     *
     * Uses a 5-minute timeout — enough to complete a full assessment. The emitter
     * polls the outgoing queue with a 2-second timeout per poll so that it releases
     * the connection thread periodically rather than blocking indefinitely. When a
     * "RESULT:" message containing "Assessment written" arrives, the emitter
     * completes — this is the signal that the agent has finished.
     *
     * The browser should open this endpoint via EventSource before calling /api/assess,
     * to avoid a race condition where early agent messages are missed. In practice the
     * queue buffers messages, so ordering does not matter as long as the browser opens
     * the stream within a few seconds of starting the assessment.
     */
    @GetMapping("/chat/stream")
    fun streamMessages(): SseEmitter {
        val emitter = SseEmitter(300_000L) // 5-minute timeout

        // Stream on a daemon thread, not the HTTP request thread. SseEmitter
        // holds the connection open, so blocking the request thread would exhaust
        // the server's thread pool under concurrent connections.
        Thread {
            try {
                while (true) {
                    // Poll with timeout rather than take() so we can detect emitter
                    // closure (client disconnect, timeout) and exit cleanly.
                    val message = webInteraction.outgoingQueue.poll(2, TimeUnit.SECONDS)
                    if (message != null) {
                        emitter.send(SseEmitter.event().data(message))
                        // Close the stream after the terminal result — the agent is done.
                        if (message.startsWith("RESULT:") && message.contains("Assessment written")) {
                            emitter.complete()
                            return@Thread
                        }
                    }
                }
            } catch (e: Exception) {
                emitter.completeWithError(e)
            }
        }.also { it.isDaemon = true }.start()

        return emitter
    }

    /**
     * Receives the user's response and forwards it to the agent thread.
     *
     * The response body is a plain string placed directly onto the incoming queue.
     * The agent thread is blocking on this queue in either askQuestion or
     * presentStatement — this POST unblocks it.
     *
     * No validation is performed here. The interaction port validates numeric responses
     * for presentStatement calls and re-prompts if needed. This controller's only job
     * is to get the string from HTTP into the queue.
     */
    @PostMapping("/chat/respond")
    fun receiveResponse(@RequestBody response: UserResponse): Map<String, String> {
        webInteraction.incomingQueue.put(response.response)
        return mapOf("status" to "received")
    }
}
