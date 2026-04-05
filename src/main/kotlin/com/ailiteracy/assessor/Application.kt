/**
 * The ALCI Assessor application entry point. This is the composition
 * root that boots Spring, discovers the Embabel agent, and activates
 * either the Spring Shell CLI or the web chat interface depending on
 * the active profile.
 *
 * The application does not contain business logic — it wires the
 * agent, the interaction ports, and the Spring context together.
 */
package com.ailiteracy.assessor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.ailiteracy.assessor", "com.embabel.agent.shell"])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
