package penta.view

import PentaViz
import client
import clientDispatcher
import com.lightningkite.koolui.concepts.Animation
import com.lightningkite.koolui.concepts.Importance
import com.lightningkite.koolui.concepts.TextInputType
import com.lightningkite.koolui.concepts.TextSize
import com.lightningkite.koolui.views.basic.text
import com.lightningkite.koolui.views.interactive.button
import com.lightningkite.koolui.views.layout.horizontal
import com.lightningkite.koolui.views.layout.space
import com.lightningkite.koolui.views.layout.vertical
import com.lightningkite.reacktive.list.MutableObservableList
import com.lightningkite.reacktive.list.mutableObservableListOf
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.transform
import io.ktor.client.features.websocket.webSocket
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.http.content.TextContent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.serialization.list
import mu.KotlinLogging
import penta.LoginState
import penta.SerialNotation
import penta.json
import penta.network.GameSessionInfo
import penta.network.LoginRequest
import penta.network.LoginResponse
import penta.network.ServerStatus
import penta.util.authenticateWith
import penta.util.authenticatedRequest
import penta.util.parse
import penta.util.suspendInfo

class MultiplayerVG<VIEW>() : MyViewGenerator<VIEW> {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun login(
        urlInput: String,
        userIdInput: String,
        passwordInput: String?,
        onSuccess: () -> Unit = {}
    ) {
        val baseURL = Url(urlInput)
        login(baseURL, userIdInput, passwordInput, onSuccess)
    }

    fun login(
        baseURL: Url,
        userIdInput: String,
        passwordInput: String?,
        onSuccess: () -> Unit = {}
    ) {
        GlobalScope.launch {
            val url = URLBuilder(baseURL).apply {
                path("api", "status")
            }.build()
            logger.info { "url: $url" }
            val status = try {
                client.request<ServerStatus>(url) {}
            } catch (exception: Exception) {
                logger.error(exception) { "request failed" }
                null
            }

            logger.info { "status: $status" }
            if (status != null) {
                val loginUrl = URLBuilder(baseURL).apply {
                    path("api", "login")
                }.build()
                val (loginResponse, sessionId) = client.post<HttpResponse>(loginUrl) {
                    body = TextContent(
                        text = json.stringify(
                            LoginRequest.serializer(),
                            LoginRequest(
                                userId = userIdInput,
                                password = passwordInput
                            )
                        ),
                        contentType = ContentType.Application.Json
                    )
                }.run {
                    logger.debug { "headers: $headers" }
                    parse(LoginResponse.serializer()) to headers["SESSION"]
                }
                PentaViz.gameState.multiplayerState.value = when (loginResponse) {
                    is LoginResponse.UserIdRejected -> {
                        LoginState.UserIDRejected(
                            baseUrl = baseURL,
                            userId = userIdInput,
                            reason = loginResponse.reason
                        )
                    }
                    is LoginResponse.IncorrectPassword -> LoginState.RequiresPassword(
                        baseUrl = baseURL,
                        userId = userIdInput
                    )
                    is LoginResponse.Success -> LoginState.Connected(
                        baseUrl = baseURL,
                        userId = userIdInput,
                        session = sessionId ?: throw IllegalStateException("missing SESSION header")
                    ).also { state ->
                        onSuccess()

                        val whoAmIUrl = URLBuilder(baseURL).apply {
                            path("whoami")
                        }.build()
                        client.authenticatedRequest(whoAmIUrl, state, HttpMethod.Get) {
                            authenticateWith(state)
                        }.run {
                            logger.suspendInfo { readText() }
                        }
                    }
                }
            }
        }
    }

    fun listGames(
        state: LoginState.Connected,
        gamesList: MutableObservableList<GameSessionInfo>,
        onSuccess: () -> Unit = {}
    ) {
        GlobalScope.launch {
            val listGamesUrl = URLBuilder(state.baseUrl)
                .path("api", "games")
                .build()

            val receivedList =
                client.authenticatedRequest(listGamesUrl, state, HttpMethod.Get, GameSessionInfo.serializer().list)
            gamesList.replace(receivedList)
            onSuccess()
        }
    }

    fun joinGame(state: LoginState.Connected, game: GameSessionInfo) {
        val wsUrl = URLBuilder(state.baseUrl)
            .path("ws", "game", game.id)
            .build()
        GlobalScope.launch(clientDispatcher) {
            client.webSocket(
                method = HttpMethod.Get,
                host = "127.0.0.1",
                port = 55555, path = "/ws/game/${game.id}",
//                urlString = wsUrl.toString(),
                request = {
                    authenticateWith(state)
//                    parameter("gameId", game.id)
                }
            ) {
                logger.info { "connection opened" }
//                PentaViz.resetBoard()
                PentaViz.gameState.multiplayerState.value = LoginState.Playing(
                    baseUrl = state.baseUrl,
                    userId = state.userId,
                    session = state.session,
                    gameId = game.id,
                    websocketSession = this
                ).also {
                    logger.info { "setting multiplayerStatus to $it" }
                }
                try {
                    while (true) {
                        val notationJson = (incoming.receive() as Frame.Text).readText()

                        logger.info { "receiving notation $notationJson" }
                        val notation = json.parse(SerialNotation.serializer(), notationJson)
                        notation.asMove(PentaViz.gameState).also {
                            // apply move
                            PentaViz.gameState.processMove(it)
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    val reason = closeReason.await()
                    logger.debug(e) { "onClose $reason" }
                    // TODO transition to state `ConnectionLost`
                } catch (e: Throwable) {
                    val reason = closeReason.await()
                    logger.error(e) { "onClose $reason" }
                    // TODO transition to state `ConnectionLost`
                } finally {
                    logger.info { "connection closing" }
                }
            }

            // connection closed "normally" ?
//            PentaViz.gameState.multiplayerState.value = LoginState.Connected(
//                baseUrl = state.baseUrl,
//                userId = state.userId,
//                session = state.session
//            )
        }
    }

    override fun generate(dependency: MyViewFactory<VIEW>): VIEW = with(dependency) {
        swap(
            PentaViz.gameState.multiplayerState.transform { state ->
                when (state) {
                    is LoginState.Disconnected, is LoginState.UserIDRejected -> {
                        val urlInput = StandardObservableProperty(state.baseUrl.toString())
                        val userIdInput = StandardObservableProperty(state.userId)

                        vertical {
                            +space()
                            if (state is LoginState.UserIDRejected) {
                                -card(
                                    text(
                                        text = state.reason,
                                        size = TextSize.Body.bigger,
                                        importance = Importance.Danger
                                    )
                                )//.background(theme.importance(Importance.Danger).background)
                            }
                            -text("Username:")
                            -textField(
                                text = userIdInput,
                                type = TextInputType.Name
                            ).background(theme.main.background)
                            -text("Enter Server URL:")
                            -horizontal {
                                +textField(
                                    text = urlInput,
                                    placeholder = "localhost",
                                    type = TextInputType.URL
                                ).background(theme.main.background)
                                -button(
                                    label = "Connect",
                                    onClick = {
                                        login(urlInput.value, userIdInput.value, null)
                                    }
                                )
                            }.setHeight(32f)
                            +space()
                        } to Animation.Fade
                    }
                    is LoginState.RequiresPassword -> {
                        val passwordInput = StandardObservableProperty("")

                        vertical {
                            +space()
                            -text("Enter password")
                            -horizontal {
                                +textField(
                                    text = passwordInput,
                                    type = TextInputType.Password
                                ).background(theme.main.background)
                                -button(
                                    label = "Login",
                                    onClick = {
                                        login(state.baseUrl, state.userId, passwordInput.value)
                                    }
                                )
                            }.setHeight(32f)
                            -button(
                                label = "back",
                                onClick = {
                                    PentaViz.gameState.multiplayerState.value = LoginState.Disconnected(
                                        baseUrl = state.baseUrl,
                                        userId = state.userId
                                    )
                                }
                            )
                            +space()

                        } to Animation.Fade
                    }
                    is LoginState.Connected -> {
                        //TODO: receive games list initially
                        val games = mutableObservableListOf<GameSessionInfo>()
                        games.onListUpdate.add {
                            logger.info { "list updated: ${it.joinToString()}" }
                        }
                        listGames(state, games)
                        val refreshing = StandardObservableProperty(false)

                        refresh(
                            contains = vertical {
                                -horizontal {
                                    +text(
                                        "Connected with ${state.baseUrl}"
                                    )
                                    -button(
                                        label = "Disconnect",
                                        onClick = {
                                            PentaViz.gameState.multiplayerState.value = LoginState.Disconnected(
                                                baseUrl = state.baseUrl,
                                                userId = state.userId
                                            )
                                        }
                                    )
                                }
                                +list(
                                    data = games,
                                    makeView = { obs, indexProp ->
                                        val game = obs.value
                                        game.run {
                                            horizontal {
                                                -vertical {
                                                    -text("id: $id")
                                                    -text("running: $running")
                                                }
                                                +space()
                                                -vertical {
                                                    +text("players: ${players.size}")
                                                    +text("observers: ${observers.size}")
                                                }
                                                -button(
                                                    label = "Join",
                                                    onClick = {
                                                        joinGame(state, game)
                                                    }
                                                )
                                            }
                                        }

                                    }
                                )
                            },
                            working = refreshing,
                            onRefresh = {
                                refreshing.value = true

                                // TODO: receive fresh game list from server
                                listGames(state, games) {
                                    refreshing.value = false
                                }
                            }
                        ).setWidth(200f) to Animation.Fade
                    }
                    is LoginState.Playing -> {
                        vertical {
                            +space()
                            -button(
                                label = "Leave Game",
                                onClick = {
                                    PentaViz.gameState.multiplayerState.value = LoginState.Connected(
                                        baseUrl = state.baseUrl,
                                        userId = state.userId,
                                        session = state.session
                                    )
                                }
                            )
                        } to Animation.Fade
                    }
                }
            }
        )
    }
}
