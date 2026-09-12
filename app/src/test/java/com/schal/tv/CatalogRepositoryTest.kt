package com.schal.tv

import com.schal.tv.catalog.CatalogRepository
import com.schal.tv.core.StreamStatus
import com.schal.tv.core.StreamType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryTest {

    private val validCatalog = """
        {
          "schema_version": "1.0",
          "categories": {
            "tv": [
              {
                "id": "tv_001",
                "type": "tv",
                "name": "Chaîne avec flux",
                "country": "BJ",
                "language": "fr",
                "stream_url": "https://example.com/channel.m3u8",
                "stream_type": "hls",
                "is_live": true,
                "is_active": true,
                "offline_available": false,
                "updated_at": "2026-09-12T00:00:00Z"
              },
              {
                "id": "tv_002",
                "type": "tv",
                "name": "Chaîne sans flux",
                "country": "BJ",
                "language": "fr",
                "stream_url": "",
                "stream_type": "unknown",
                "is_live": true,
                "is_active": true,
                "offline_available": false,
                "updated_at": "2026-09-12T00:00:00Z"
              },
              {
                "id": "tv_003",
                "type": "tv",
                "name": "Chaîne inactive",
                "is_active": false,
                "stream_url": "https://example.com/inactive.m3u8"
              }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `catalogue vide ne produit aucune chaine`() {
        val emptyCatalog = """{"schema_version":"1.0","categories":{"tv":[]}}"""
        val channels = CatalogRepository.parseChannels(emptyCatalog)
        assertTrue(channels.isEmpty())
    }

    @Test
    fun `chaine sans id ou sans nom est rejetee`() {
        val catalog = """
            {"schema_version":"1.0","categories":{"tv":[
                {"id":"", "name":"Sans id"},
                {"id":"tv_x", "name":""}
            ]}}
        """.trimIndent()
        val channels = CatalogRepository.parseChannels(catalog)
        assertTrue(channels.isEmpty())
    }

    @Test
    fun `chaines inactives sont filtrees`() {
        val channels = CatalogRepository.parseChannels(validCatalog)
        assertEquals(2, channels.size)
        assertTrue(channels.none { it.id == "tv_003" })
    }

    @Test
    fun `chaine sans flux garde stream_url vide et n'est pas fabriquee`() {
        val channels = CatalogRepository.parseChannels(validCatalog)
        val withoutStream = channels.first { it.id == "tv_002" }
        assertFalse(withoutStream.hasConfiguredStream())
        assertEquals("", withoutStream.streamUrl)
        assertEquals(StreamType.UNKNOWN, withoutStream.streamType)
    }

    @Test
    fun `chaine avec flux est correctement typee`() {
        val channels = CatalogRepository.parseChannels(validCatalog)
        val withStream = channels.first { it.id == "tv_001" }
        assertTrue(withStream.hasConfiguredStream())
        assertEquals(StreamType.HLS, withStream.streamType)
        assertEquals(StreamStatus.UNKNOWN, withStream.streamStatus) // jamais "online" sans vérification réelle
    }

    @Test
    fun `json malforme leve une exception geree par le repository`() {
        var threw = false
        try {
            CatalogRepository.parseChannels("{ceci n'est pas du json}")
        } catch (e: org.json.JSONException) {
            threw = true
        }
        assertTrue(threw)
    }
}
