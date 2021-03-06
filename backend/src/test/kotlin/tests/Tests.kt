package tests

import actions.Action
import mu.KotlinLogging
import org.junit.Test
import org.reduxkotlin.applyMiddleware
import org.reduxkotlin.createStore
import penta.PentaMove
import penta.PlayerState
import penta.redux_rewrite.BoardState

class Tests {
    private val logger = KotlinLogging.logger {}
    @Test
    fun test() {
        val boardStore: org.reduxkotlin.Store<BoardState> = createStore(
            BoardState.reducer,
            BoardState.create(
                // TODO: remove default users
                listOf(PlayerState("alice", "cross"), PlayerState("bob", "triangle")),
                BoardState.GameType.TWO
            ),
            applyMiddleware(/*loggingMiddleware(logger)*/)
        )
        logger.info { "initialized" }
        boardStore.dispatch(Action(PentaMove.PlayerJoin(PlayerState("eve", "square"))))
        boardStore.dispatch(Action(PentaMove.InitGame))
    }

}