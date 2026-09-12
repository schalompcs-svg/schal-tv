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
import java.net.HttpURLConnection
import java.net.URL

class CatalogRepository(private val context: Context) {

    companion object {
        const val LOCAL_CATALOG_FILENAME = "schalom_catalog.json"

        private const val REMOTE_CATALOG =
            "https://raw.githubusercontent.com/schalompcs-svg/schal-tv/main/catalog/channels-valid.json"

        fun parseChannels(rawJson: String): List<TvChannel> {
            val root = JSONObject(rawJson)
            val categories = root.optJSONObject("categories") ?: return emptyList()
            val tvArray: JSONArray = categories.optJSONArray("tv") ?: JSONArray()

            val result = mutableListOf<TvChannel>()

            for (i in 0 until tvArray.length()) {
                val obj = tvArray.optJSONObject(i) ?: continue

                val id = obj.optString("id", "").trim()
                val name = obj.optString("name", "").trim()

                if (id.isEmpty() || name.isEmpty()) continue

                val categoriesArray = obj.optJSONArray("categories")
                val category = when {
                    categoriesArray != null && categoriesArray.length() > 0 ->
                        categoriesArray.optString(0, "")
                    else -> obj.optString("category", "")
                }

                val alternate = mutableListOf<String>()
                val streams = obj.optJSONArray("streams")
                if (streams != null) {
                    for (j in 0 until streams.length()) {
                        val u = streams.optString(j, "").trim()
                        if (u.isNotEmpty()) alternate += u
                    }
                }

                result += TvChannel(
                    id = id,
                    name = name,
                    description = obj.optString("description", ""),
                    country = obj.optString("country", ""),
                    language = obj.optString("language", ""),
                    logo = obj.optString("logo", ""),
                    category = category,
                    genre = obj.optString("genre", ""),
                    streamUrl = obj.optString("stream_url", ""),
                    alternateStreams = alternate.distinct(),
                    streamType = StreamType.fromString(
                        obj.optString("stream_type", "unknown")
                    ),
                    quality = obj.optString("quality", ""),
                    isLive = obj.optBoolean("is_live", true),
                    isActive = obj.optBoolean("is_active", true),
                    offlineAvailable = obj.optBoolean("offline_available", false),
                    favorite = false,
                    updatedAt = obj.optString("updated_at", ""),
                    streamStatus = StreamStatus.fromString(
                        obj.optString("stream_status", "unknown")
                    )
                )
            }

            return result
                .filter { it.isActive }
                .distinctBy { it.id }
                .sortedBy { it.name.lowercase() }
        }
    }

    private fun localCatalogFile(): File =
        File(context.filesDir, LOCAL_CATALOG_FILENAME)

    fun saveDownloadedCatalog(rawJson: String) {
        localCatalogFile().writeText(rawJson, Charsets.UTF_8)
    }

    fun loadCatalog(): CatalogResult {
        val local = localCatalogFile()

        val rawJson = try {
            when {
                local.exists() ->
                    local.readText(Charsets.UTF_8)

                else ->
                    context.assets.open(LOCAL_CATALOG_FILENAME)
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
            }
        } catch (e: IOException) {
            return CatalogResult.Error(
                "Impossible de lire le catalogue local",
                e
            )
        }

        return try {
            val channels = parseChannels(rawJson)

            if (channels.isEmpty()) {
                CatalogResult.Empty("AUCUNE CHAÎNE DISPONIBLE")
            } else {
                CatalogResult.Success(channels)
            }
        } catch (e: JSONException) {
            CatalogResult.Error(
                "Catalogue SCHAL-TV invalide",
                e
            )
        }
    }

    fun refreshFromInternet(): CatalogResult {
        var connection: HttpURLConnection? = null

        return try {
            connection = URL(REMOTE_CATALOG)
                .openConnection() as HttpURLConnection

            connection.connectTimeout = 10000
            connection.readTimeout = 20000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "SCHAL-TV/1.0")

            val code = connection.responseCode

            if (code !in 200..299) {
                return CatalogResult.Error(
                    "Catalogue distant indisponible : HTTP $code"
                )
            }

            val raw = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val channels = parseChannels(raw)

            if (channels.isEmpty()) {
                CatalogResult.Empty("Catalogue distant vide")
            } else {
                saveDownloadedCatalog(raw)
                CatalogResult.Success(channels)
            }
        } catch (e: Exception) {
            CatalogResult.Error(
                "Actualisation impossible : ${e.message ?: "erreur réseau"}",
                e
            )
        } finally {
            connection?.disconnect()
        }
    }
}
