package com.squashscore.model

/**
 * Supported racket sports. Stored as name string in Match to
 * avoid kotlinx.serialization annotations on model classes.
 *
 * @param defaultTarget  0 = standard tennis/padel scoring (indefinite mode);
 *                       non-zero = rally-point target for game win.
 *   Squash         11   (PAR to 11)
 *   Badminton      21   (rally to 21, best of 3)
 *   Tennis          0   (15-30-40 — no rally-point target)
 *   Table Tennis   11   (first to 11)
 *   Pickleball     11   (first to 11)
 *   Racquetball    15   (first to 15)
 *   Padel           0   (same scoring as tennis)
 */
enum class Sport(val displayName: String, val defaultTarget: Int = 11) {
    SQUASH("Squash", 11),
    BADMINTON("Badminton", 21),
    TENNIS("Tennis", 0),
    TABLE_TENNIS("Table Tennis", 11),
    PICKLEBALL("Pickleball", 11),
    RACQUETBALL("Racquetball", 15),
    PADEL("Padel", 0);

    /** True when the sport uses non-rally-point scoring (tennis/padel). */
    val usesStandardScoring get() = defaultTarget > 0

    companion object {
        fun fromIndex(i: Int): Sport = entries[i % entries.size]
        fun fromName(name: String): Sport =
            entries.firstOrNull { it.name == name } ?: SQUASH
    }
}
