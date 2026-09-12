package com.schal.tv.storage

import android.content.Context
import com.schal.tv.core.Favorites
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Favoris utilisateur (favorites.json), stockés localement et séparément du
 * catalogue SCHALOM. Une mise à jour du catalogue distant ne doit jamais
 * effacer ce fichier.
 */
class FavoritesStore(context: Context) {

    private val file = File(context.filesDir, "favorites.json")

    fun load(): Favorites {
        if (!file.exists()) return Favorites()
        return try {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val favObj = root.optJSONObject("favorites") ?: return Favorites()
            Favorites(
                tv = toSet(favObj.optJSONArray("tv")),
                radio = toSet(favObj.optJSONArray("radio")),
                localVideo = toSet(favObj.optJSONArray("local_video"))
            )
        } catch (e: Exception) {
            Favorites()
        }
    }

    fun save(favorites: Favorites) {
        val favObj = JSONObject()
            .put("tv", JSONArray(favorites.tv.toList()))
            .put("radio", JSONArray(favorites.radio.toList()))
            .put("local_video", JSONArray(favorites.localVideo.toList()))
        val root = JSONObject().put("favorites", favObj)
        file.writeText(root.toString(2), Charsets.UTF_8)
    }

    fun toggleTvFavorite(channelId: String): Boolean {
        val favorites = load()
        val nowFavorite = if (favorites.tv.contains(channelId)) {
            favorites.tv.remove(channelId); false
        } else {
            favorites.tv.add(channelId); true
        }
        save(favorites)
        return nowFavorite
    }

    private fun toSet(array: JSONArray?): MutableSet<String> {
        val set = mutableSetOf<String>()
        if (array == null) return set
        for (i in 0 until array.length()) set += array.optString(i)
        return set
    }
}
