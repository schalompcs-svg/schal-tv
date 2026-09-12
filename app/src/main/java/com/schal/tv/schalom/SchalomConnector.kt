package com.schal.tv.schalom

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/**
 * Connecteur vers l'API/catalogue distant SCHALOM. INTERNET NÉCESSAIRE.
 *
 * Aucune URL de flux n'est jamais fabriquée ici : ce connecteur se contente
 * de relayer tel quel ce que retourne le serveur SCHALOM, ou de signaler un
 * échec explicite (timeout, indisponible, réponse invalide).
 *
 * L'adresse de base doit être fournie par la configuration de déploiement
 * réelle (pas de valeur par défaut inventée) — voir docs/INSTALLATION.md.
 */
sealed class SchalomFetchResult {
    data class Success(val rawJson: String) : SchalomFetchResult()
    data class NetworkUnavailable(val message: String = "INTERNET NÉCESSAIRE") : SchalomFetchResult()
    data class ServerError(val message: String, val httpCode: Int? = null) : SchalomFetchResult()
    data class Timeout(val message: String = "Serveur SCHALOM indisponible (timeout)") : SchalomFetchResult()
}

class SchalomConnector(
    private val context: Context,
    private val baseUrl: String?,
    private val connectTimeoutMs: Int = 8000,
    private val readTimeoutMs: Int = 8000
) {

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Récupère le catalogue distant. Ne lance jamais d'exception non gérée. */
    fun fetchCatalog(): SchalomFetchResult {
        if (baseUrl.isNullOrBlank()) {
            return SchalomFetchResult.ServerError("Adresse SCHALOM non configurée")
        }
        if (!isNetworkAvailable()) {
            return SchalomFetchResult.NetworkUnavailable()
        }

        var connection: HttpURLConnection? = null
        return try {
            val url = URL("$baseUrl/api/catalog")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
            }
            val code = connection.responseCode
            if (code in 200..299) {
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                SchalomFetchResult.Success(body)
            } else {
                SchalomFetchResult.ServerError("Réponse SCHALOM invalide", code)
            }
        } catch (e: SocketTimeoutException) {
            SchalomFetchResult.Timeout()
        } catch (e: IOException) {
            SchalomFetchResult.ServerError(e.message ?: "Erreur réseau SCHALOM")
        } finally {
            connection?.disconnect()
        }
    }
}
