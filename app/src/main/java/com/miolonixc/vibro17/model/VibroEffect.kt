package com.miolonixc.vibro17.model

/**
 * Description of a single vibration effect.
 *
 * [timings]   – durations in milliseconds (alternating OFF/ON for the first entry).
 * [amplitudes] – vibration strength per entry, 0..255 (ignored on API < 26).
 * [repeat]    – index in the arrays to loop from (-1 = play once).
 */
data class VibroEffect(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val timings: LongArray,
    val amplitudes: IntArray,
    val repeat: Int = 0,
    val category: String = "tech"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VibroEffect
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
