package com.squashscore.model

import java.time.Instant
import java.util.UUID

/**
 * Core match state and scoring logic.
 */
data class Match(
    val id: String = UUID.randomUUID().toString(),
    val state: GameState = GameState.SETUP,
    val createdAt: Instant = Instant.now(),
    val players: List<Player> = emptyList(),
    val pointsToWin: Int = 11,
    val bestOf: Int = 5,
    val indefinite: Boolean = false,
    val selfScoreOnly: Boolean = false,
    val simpleMode: Boolean = false,
    val sportName: String = Sport.SQUASH.name,
    val currentGame: Int = 1,
    val serverIndex: Int = 0,
    val receiverIndex: Int = 1,
    val waitingIndex: Int? = null,
    val pointHistory: List<PointEvent> = emptyList(),
    val completedGames: List<GameResult> = emptyList()
) {
    val isThreePlayer get() = players.size == 3
    val server get() = players[serverIndex]
    val receiver get() = players[receiverIndex]
    val waitingPlayer get() = waitingIndex?.let { players[it] }
    val gamesNeeded get() = if (indefinite) 1 else bestOf / 2 + 1
    val isMatchOver get() = state == GameState.FINISHED
    val winner get() = if (isMatchOver) players.maxByOrNull { it.gamesWon } else null

    // ── Setup ──

    fun setupTwoPlayer(
        playerA: String, playerB: String,
        pointsToWin: Int = 11, bestOf: Int = 5,
        indefinite: Boolean = false,
        simpleMode: Boolean = false,
        sportName: String = Sport.SQUASH.name
    ): Match = copy(
        players = listOf(Player(name = playerA), Player(name = playerB)),
        pointsToWin = pointsToWin,
        bestOf = bestOf,
        indefinite = indefinite,
        simpleMode = simpleMode,
        sportName = sportName,
        state = GameState.WARMUP,
        serverIndex = 0, receiverIndex = 1, waitingIndex = null
    )

    fun setupThreePlayer(
        playerA: String, playerB: String, playerC: String,
        pointsToWin: Int = 11,
        indefinite: Boolean = false,
        selfScoreOnly: Boolean = false,
        simpleMode: Boolean = false,
        sportName: String = Sport.SQUASH.name
    ): Match = copy(
        players = listOf(Player(name = playerA), Player(name = playerB), Player(name = playerC)),
        pointsToWin = pointsToWin,
        bestOf = 1,
        indefinite = indefinite,
        selfScoreOnly = selfScoreOnly,
        simpleMode = simpleMode,
        sportName = sportName,
        state = GameState.WARMUP,
        serverIndex = 0, receiverIndex = 1, waitingIndex = 2
    )

    fun startMatch(): Match = copy(state = GameState.PLAYING)

    // ── Scoring ──

    fun awardPoint(playerIndex: Int): Match? {
        if (state != GameState.PLAYING) return null

        // Simple mode (2-player) or self-score (3-player): just increment, no game logic
        if (simpleMode || selfScoreOnly) {
            val scorer = if (selfScoreOnly) 0 else playerIndex
            val event = PointEvent(
                scorerIndex = scorer,
                previousServer = serverIndex,
                previousReceiver = receiverIndex,
                previousWaiting = waitingIndex,
                previousScores = players.map { it.score }
            )
            return copy(
                pointHistory = pointHistory + event,
                players = players.mapIndexed { i, p ->
                    if (i == scorer) p.copy(score = p.score + 1) else p
                }
            )
        }

        if (playerIndex != serverIndex && playerIndex != receiverIndex) return null

        val event = PointEvent(
            scorerIndex = playerIndex,
            previousServer = serverIndex,
            previousReceiver = receiverIndex,
            previousWaiting = waitingIndex,
            previousScores = players.map { it.score }
        )

        val updated = copy(
            pointHistory = pointHistory + event,
            players = players.mapIndexed { i, p ->
                if (i == playerIndex) p.copy(score = p.score + 1) else p
            }
        )

        return if (isThreePlayer) updated.handleThreePlayerPoint(playerIndex)
        else updated.handleTwoPlayerPoint(playerIndex)
    }

    fun undoPoint(): Match? {
        val last = pointHistory.lastOrNull() ?: return null
        if (state != GameState.PLAYING && state != GameState.BETWEEN_GAMES) return null
        return copy(
            players = players.mapIndexed { i, p ->
                p.copy(score = last.previousScores.getOrElse(i) { p.score })
            },
            serverIndex = last.previousServer,
            receiverIndex = last.previousReceiver,
            waitingIndex = last.previousWaiting,
            pointHistory = pointHistory.dropLast(1),
            state = if (state == GameState.BETWEEN_GAMES) GameState.PLAYING else state
        )
    }

    // ── Manual end ──

    fun endManually(): Match {
        val currentLeader = players.withIndex().maxByOrNull { (_, p) -> p.score }
        val leaderIndex = currentLeader?.index ?: 0
        val result = GameResult(
            gameNumber = currentGame,
            scores = players.map { it.score },
            winnerIndex = leaderIndex
        )
        val updatedPlayers = players.mapIndexed { i, p ->
            if (i == leaderIndex) p.copy(gamesWon = p.gamesWon + 1, score = 0)
            else p.copy(score = 0)
        }
        return copy(
            state = GameState.FINISHED,
            players = updatedPlayers,
            completedGames = completedGames + result
        )
    }

    // ── Private scoring helpers ──

    private fun handleTwoPlayerPoint(scorerIndex: Int): Match {
        if (scorerIndex == serverIndex) {
            return if (isGameWon(scorerIndex)) finishGame(scorerIndex) else this
        } else {
            return copy(serverIndex = receiverIndex, receiverIndex = serverIndex)
        }
    }

    private fun handleThreePlayerPoint(scorerIndex: Int): Match {
        if (scorerIndex == serverIndex) {
            return if (isGameWon(scorerIndex)) finishGame(scorerIndex)
            else this
        } else {
            val oldServer = serverIndex
            return copy(
                serverIndex = receiverIndex,
                receiverIndex = waitingIndex!!,
                waitingIndex = oldServer
            )
        }
    }

    private fun isGameWon(playerIndex: Int): Boolean {
        if (indefinite) return false
        val winnerScore = players[playerIndex].score
        if (winnerScore < pointsToWin) return false
        return players.indices
            .filter { it != playerIndex }
            .all { winnerScore - players[it].score >= 2 }
    }

    private fun finishGame(winnerIndex: Int): Match {
        val result = GameResult(
            gameNumber = currentGame,
            scores = players.map { it.score },
            winnerIndex = winnerIndex
        )
        val updatedPlayers = players.mapIndexed { i, p ->
            if (i == winnerIndex) p.copy(gamesWon = p.gamesWon + 1, score = 0)
            else p.copy(score = 0)
        }
        return if (updatedPlayers[winnerIndex].gamesWon >= gamesNeeded) {
            copy(state = GameState.FINISHED, players = updatedPlayers, completedGames = completedGames + result)
        } else {
            copy(
                state = GameState.BETWEEN_GAMES, players = updatedPlayers,
                completedGames = completedGames + result, currentGame = currentGame + 1,
                serverIndex = if (isThreePlayer) winnerIndex else completedGames.size % 2,
                receiverIndex = if (isThreePlayer) (winnerIndex + 1) % 3 else 1 - (completedGames.size % 2),
                waitingIndex = if (isThreePlayer) (winnerIndex + 2) % 3 else null
            )
        }
    }
}

data class PointEvent(
    val scorerIndex: Int,
    val previousServer: Int,
    val previousReceiver: Int,
    val previousWaiting: Int?,
    val previousScores: List<Int>
)

data class GameResult(
    val gameNumber: Int,
    val scores: List<Int>,
    val winnerIndex: Int
)
