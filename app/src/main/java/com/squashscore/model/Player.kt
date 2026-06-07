package com.squashscore.model

data class Player(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val score: Int = 0,
    val gamesWon: Int = 0
)
