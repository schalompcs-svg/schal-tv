package com.schal.tv.cache

import android.content.Context
import com.schal.tv.core.CacheItem
import com.schal.tv.core.CacheManifest
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Gère cache_manifest.json : suit quelles entrées du catalogue ont été
 * récupérées récemment et jusqu'à quand elles sont considérées valides.
 * Ne stocke aucune donnée de chaîne elle-même — uniquement des métadonnées
 * de fraîcheur (le contenu réel reste dans schalom_catalog.json local).
 */
class CacheManager(context: Context) {

    private val file = File(context.filesDir, "cache_manifest.json")
    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun load(): CacheManifest {
        if (!file.exists()) return CacheManifest()
        return try {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val manifest = CacheManifest(schemaVersion = root.optString("schema_version", "1.0"))
            val items: JSONArray = root.optJSONArray("items") ?: JSONArray()
            for (i in 0 until items.length()) {
                val obj = items.optJSONObject(i) ?: continue
                manifest.items += CacheItem(
                    id = obj.optString("id", ""),
                    cachedAt = obj.optString("cached_at", ""),
                    expiresAt = obj.optString("expires_at", ""),
                    status = obj.optString("status", "valid")
                )
            }
            manifest
        } catch (e: Exception) {
            // Cache corrompu : on repart d'un cache vide plutôt que de crasher.
            CacheManifest()
        }
    }

    fun save(manifest: CacheManifest) {
        val root = JSONObject()
        root.put("schema_version", manifest.schemaVersion)
        val itemsArray = JSONArray()
        manifest.items.forEach { item ->
            itemsArray.put(
                JSONObject()
                    .put("id", item.id)
                    .put("cached_at", item.cachedAt)
                    .put("expires_at", item.expiresAt)
                    .put("status", item.status)
            )
        }
        root.put("items", itemsArray)
        file.writeText(root.toString(2), Charsets.UTF_8)
    }

    fun markCached(channelId: String, ttlHours: Long = 24) {
        val manifest = load()
        val now = Date()
        val expires = Date(now.time + ttlHours * 3_600_000L)
        manifest.items.removeAll { it.id == channelId }
        manifest.items += CacheItem(
            id = channelId,
            cachedAt = iso.format(now),
            expiresAt = iso.format(expires),
            status = "valid"
        )
        save(manifest)
    }

    fun isValid(channelId: String): Boolean {
        val entry = load().items.find { it.id == channelId } ?: return false
        return try {
            entry.status == "valid" && iso.parse(entry.expiresAt)?.after(Date()) == true
        } catch (e: Exception) {
            false
        }
    }
}
