package com.squashscore.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Player(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val score: Int = 0,
    val gamesWon: Int = 0
)
