package com.schal.tv.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * Préférences légères : volume courant, dernière chaîne lue, historique de
 * lecture, position de reprise. Utilise SharedPreferences (aucune dépendance
 * lourde) — cohérent avec l'exigence de faible consommation mémoire.
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("schal_tv_prefs", Context.MODE_PRIVATE)

    var volumePercent: Int
        get() = sp.getInt(KEY_VOLUME, DEFAULT_VOLUME)
        set(value) = sp.edit().putInt(KEY_VOLUME, value.coerceIn(0, 100)).apply()

    var lastChannelId: String?
        get() = sp.getString(KEY_LAST_CHANNEL, null)
        set(value) = sp.edit().putString(KEY_LAST_CHANNEL, value).apply()

    fun resumePositionMs(itemId: String): Long = sp.getLong(KEY_PREFIX_RESUME + itemId, 0L)

    fun setResumePositionMs(itemId: String, positionMs: Long) {
        sp.edit().putLong(KEY_PREFIX_RESUME + itemId, positionMs).apply()
    }

    fun pushHistory(itemId: String, maxEntries: Int = 50) {
        val current = sp.getString(KEY_HISTORY, "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        val updated = (listOf(itemId) + current.filter { it != itemId }).take(maxEntries)
        sp.edit().putString(KEY_HISTORY, updated.joinToString(",")).apply()
    }

    fun history(): List<String> =
        sp.getString(KEY_HISTORY, "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    companion object {
        private const val KEY_VOLUME = "volume_percent"
        private const val KEY_LAST_CHANNEL = "last_channel_id"
        private const val KEY_HISTORY = "history"
        private const val KEY_PREFIX_RESUME = "resume_"
        private const val DEFAULT_VOLUME = 70
    }
}
