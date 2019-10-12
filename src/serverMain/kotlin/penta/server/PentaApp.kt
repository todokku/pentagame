package penta.server

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.sessions.SessionStorageMemory
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.sessions.Sessions
import io.ktor.sessions.header
import io.ktor.websocket.WebSockets
import java.time.Duration

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)

    install(WebSockets)

//    install(HttpsRedirect)
//    install(HSTS)
    install(CORS) {
        anyHost()
        maxAge = Duration.ofMinutes(20)
    }
//    install(Metrics) {
//        val reporter = Slf4jReporter.forRegistry(registry)
//                .outputTo(log)
//                .convertRatesTo(TimeUnit.SECONDS)
//                .convertDurationsTo(TimeUnit.MILLISECONDS)
//                .build()
//        reporter.start(10, TimeUnit.SECONDS)
//    }
    install(ContentNegotiation) {
        jackson {
            registerModule(KotlinModule()) // Enable Kotlin support
            enable(SerializationFeature.INDENT_OUTPUT)
//            enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
        }
    }
    install(Sessions) {
        header<UserSession>("SESSION", storage = SessionStorageMemory()) {
//            cookie.path = "/" // Specify cookie's path '/' so it can be used in the whole site
        }
    }

}