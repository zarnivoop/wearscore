package com.squashscore.model

/**
 * Supported racket sports. Stored as name string in Match to
 * avoid kotlinx.serialization annotations on model classes.
 */
enum class Sport(val displayName: String) {
    SQUASH("Squash"),
    BADMINTON("Badminton"),
    TENNIS("Tennis"),
    TABLE_TENNIS("Table Tennis"),
    PICKLEBALL("Pickleball"),
    RACQUETBALL("Racquetball"),
    PADEL("Padel");

    companion object {
        fun fromIndex(i: Int): Sport = entries[i % entries.size]
        fun fromName(name: String): Sport =
            entries.firstOrNull { it.name == name } ?: SQUASH
    }
}
