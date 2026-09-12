package com.schal.tv.catalog

import android.content.Context
import com.schal.tv.core.CatalogResult
import com.schal.tv.core.StreamStatus
import com.schal.tv.core.StreamType
import com.schal.tv.core.TvChannel
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Source de vérité du catalogue de chaînes TV.
 *
 * Ordre de priorité offline-first (voir docs/ARCHITECTURE.md) :
 * 1. fichier catalogue local déjà téléchargé (filesDir/schalom_catalog.json)
 * 2. catalogue embarqué dans les assets (schalom_catalog.json, vide par défaut)
 * 3. rafraîchissement réseau via SchalomConnector (fait par CatalogViewModel,
 *    pas par ce repository, pour garder ce fichier utilisable hors ligne)
 *
 * RÈGLE ABSOLUE : aucune donnée absente du JSON n'est inventée. Un champ
 * manquant devient une valeur vide/par défaut neutre, jamais une valeur
 * fabriquée (voir Models.kt).
 */
class CatalogRepository(private val context: Context) {

    private fun localCatalogFile(): File = File(context.filesDir, LOCAL_CATALOG_FILENAME)

    /** Écrit un catalogue téléchargé depuis SCHALOM pour un usage hors ligne futur. */
    fun saveDownloadedCatalog(rawJson: String) {
        localCatalogFile().writeText(rawJson, Charsets.UTF_8)
    }

    fun loadCatalog(): CatalogResult {
        val local = localCatalogFile()
        val rawJson: String = try {
            when {
                local.exists() -> local.readText(Charsets.UTF_8)
                else -> context.assets.open(LOCAL_CATALOG_FILENAME)
                    .bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } catch (e: IOException) {
            return CatalogResult.Error("Impossible de lire le catalogue local", e)
        }

        return try {
            val channels = parseChannels(rawJson)
            if (channels.isEmpty()) {
                CatalogResult.Empty("AUCUN CONTENU DISPONIBLE DANS LE CATALOGUE")
            } else {
                CatalogResult.Success(channels)
            }
        } catch (e: JSONException) {
            CatalogResult.Error("Catalogue SCHALOM invalide (JSON malformé)", e)
        }
    }

    /**
     * Extrait uniquement categories.tv du schéma imposé. Les autres
     * catégories (radio, films, séries...) seront gérées par des
     * repositories dédiés lors de versions ultérieures (voir README/limites).
     */
    fun parseChannels(rawJson: String): List<TvChannel> = Companion.parseChannels(rawJson)

    companion object {
        const val LOCAL_CATALOG_FILENAME = "schalom_catalog.json"

        /**
         * Version sans dépendance à Context, pour être testable en JVM pur
         * (voir CatalogRepositoryTest) sans Robolectric.
         */
        fun parseChannels(rawJson: String): List<TvChannel> {
            val root = JSONObject(rawJson)
            val categories = root.optJSONObject("categories") ?: return emptyList()
            val tvArray: JSONArray = categories.optJSONArray("tv") ?: JSONArray()

            val result = mutableListOf<TvChannel>()
            for (i in 0 until tvArray.length()) {
                val obj = tvArray.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val name = obj.optString("name", "").trim()
                // Une chaîne sans id ou sans nom est rejetée plutôt qu'affichée
                // avec des valeurs inventées (cohérent avec validate_catalog.py).
                if (id.isEmpty() || name.isEmpty()) continue

                result += TvChannel(
                    id = id,
                    name = name,
                    description = obj.optString("description", ""),
                    country = obj.optString("country", ""),
                    language = obj.optString("language", ""),
                    logo = obj.optString("logo", ""),
                    streamUrl = obj.optString("stream_url", ""),
                    streamType = StreamType.fromString(obj.optString("stream_type", "unknown")),
                    isLive = obj.optBoolean("is_live", true),
                    isActive = obj.optBoolean("is_active", true),
                    offlineAvailable = obj.optBoolean("offline_available", false),
                    favorite = false, // les favoris viennent de FavoritesStore, jamais du catalogue distant
                    updatedAt = obj.optString("updated_at", ""),
                    streamStatus = StreamStatus.fromString(obj.optString("stream_status", "unknown"))
                )
            }
            return result.filter { it.isActive }
        }
    }
}
