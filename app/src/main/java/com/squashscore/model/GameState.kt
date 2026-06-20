package com.squashscore.model

import kotlinx.serialization.Serializable

@Serializable
enum class GameState {
    SETUP,
    WARMUP,
    PLAYING,
    BETWEEN_GAMES,
    FINISHED
}
