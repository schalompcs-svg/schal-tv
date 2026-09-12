package com.schal.tv.offline

import android.content.Context
import android.provider.MediaStore
import com.schal.tv.core.LocalVideo

/**
 * Scanne les vidéos réellement présentes sur l'appareil (stockage interne et
 * carte SD si montée) via MediaStore. HORS LIGNE : ne nécessite jamais
 * Internet. Ne fabrique jamais de chemin fictif — chaque LocalVideo retourné
 * correspond à un fichier réellement indexé par Android.
 */
class LocalMediaScanner(private val context: Context) {

    fun scanLocalVideos(): List<LocalVideo> {
        val results = mutableListOf<LocalVideo>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA, // chemin réel sur le stockage (interne ou SD)
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION
        )

        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        try {
            context.contentResolver.query(
                collection, projection, null, null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(pathCol) ?: continue
                    if (path.isBlank()) continue

                    results += LocalVideo(
                        id = "local_${cursor.getLong(idCol)}",
                        title = cursor.getString(nameCol) ?: path.substringAfterLast('/'),
                        path = path,
                        mimeType = cursor.getString(mimeCol) ?: "video/mp4",
                        sizeBytes = cursor.getLong(sizeCol),
                        durationSeconds = cursor.getLong(durationCol) / 1000
                    )
                }
            }
        } catch (e: SecurityException) {
            // Permission refusée : on retourne une liste vide plutôt que de
            // crasher. L'UI doit alors afficher "AUCUN CONTENU HORS LIGNE
            // DISPONIBLE" et, idéalement, proposer de redemander la permission.
            return emptyList()
        }

        return results
    }
}
