@file:JvmName("Hookmaster")

package jibril.web.hookmaster

import adriantodt.secure.Hmac
import adriantodt.utils.Properties
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import jibril.logging.TerminalConsoleAdaptor
import mu.KotlinLogging.logger
import redis.clients.jedis.Protocol
import java.io.File
import java.util.*

typealias Redis = redis.clients.jedis.Jedis

val log = logger {}

fun main(args: Array<String>) {
    /*
    deployment {
        server "web3"
        service group: "jibril", name: "hookmaster", serverPort: 0
        port = 0x5130
    }
    */

    TerminalConsoleAdaptor.initializeTerminal()

    embeddedServer(Netty, 0x5130) {
        val p = Properties.fromFile(File("hookmaster.properties"))

        File("logs/msgs").mkdirs()

        val redis = Redis(p["redis.url"] ?: Protocol.DEFAULT_HOST)

        val patreonSecret = p["patreon.secret"]
        val dblSecret = p["discordbots.secret"]

        routing {
            post("/webhooks/patreon") {
                val signature = call.request.headers["X-Patreon-Signature"]
                val text = call.receiveText()
                if (signature == null || !signature.equals(Hmac.digest(text, patreonSecret), ignoreCase = true)) {
                    val date = Date().toString()
                    File("logs/msgs/patreon-$date.txt").writeText(text, Charsets.UTF_8)
                    log.warn { "POST call to Patreon webhook with Bad signature!\nMessage was logged and saved as patreon-$date.txt" }

                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@post
                }

                val type = call.request.headers["X-Patreon-Event"]
                    ?.split(':', limit = 2)
                    ?.getOrNull(1)

                if (type == null) {
                    val date = Date().toString()
                    File("logs/msgs/patreon-$date.txt").writeText(text, Charsets.UTF_8)
                    log.warn { "POST call to Patreon webhook with Bad Patreon Event!\nMessage was logged and saved as patreon-$date.txt" }

                    call.respond(HttpStatusCode.BadRequest, "Bad Request")
                    return@post
                }

                redis.publish("events.patreon.$type", text)

                call.respond(HttpStatusCode.OK, "OK")
            }
            post("/webhooks/dbl") {
                val authorization = call.request.headers["Authorization"]
                val text = call.receiveText()
                if (authorization == null || authorization != dblSecret) {
                    val date = Date().toString()
                    File("logs/msgs/dbl-$date.txt").writeText(text, Charsets.UTF_8)
                    log.warn { "POST call to Patreon webhook with Bad authorization!\nMessage was logged and saved as dbl-$date.txt" }

                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@post
                }

                redis.publish("events.dbl.upvote", text)

                call.respond(HttpStatusCode.OK, "OK")
            }
            get("/") {
                call.respondText("Hello, world!", ContentType.Text.Html)
            }
        }
    }.start(wait = true)
}