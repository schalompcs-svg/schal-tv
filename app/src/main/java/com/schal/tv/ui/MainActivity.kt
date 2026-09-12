package com.schal.tv.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.schal.tv.R
import com.schal.tv.catalog.CatalogRepository
import com.schal.tv.core.CatalogResult
import com.schal.tv.core.NavKey
import com.schal.tv.core.NavigationController
import com.schal.tv.core.TvChannel
import com.schal.tv.databinding.ActivityMainBinding
import com.schal.tv.storage.FavoritesStore
import com.schal.tv.storage.Prefs
import java.util.concurrent.Executors

class MainActivity :
    AppCompatActivity(),
    NavigationController {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: CatalogRepository
    private lateinit var favoritesStore: FavoritesStore
    private lateinit var prefs: Prefs
    private lateinit var adapter: ChannelAdapter

    private val executor = Executors.newSingleThreadExecutor()

    private var allChannels: List<TvChannel> = emptyList()
    private var favoritesOnly = false
    private var category = "Toutes"
    private var letter = ""

    private val categories = listOf(
        "Toutes",
        "Français",
        "Actualités",
        "Sport",
        "Cinéma",
        "Séries",
        "Musique",
        "Documentaires",
        "Jeunesse",
        "Divertissement",
        "Culture",
        "Éducation",
        "Religion",
        "Afrique",
        "Europe",
        "International"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = CatalogRepository(this)
        favoritesStore = FavoritesStore(this)
        prefs = Prefs(this)

        adapter = ChannelAdapter(
            onChannelSelected = ::openChannel,
            onFavoriteToggle = ::toggleFavorite
        )

        binding.recyclerChannels.layoutManager =
            LinearLayoutManager(this)

        binding.recyclerChannels.adapter = adapter

        setupCategories()
        setupAlphabet()
        setupActions()

        loadCatalog()

        refreshRemoteCatalog()
    }

    private fun setupCategories() {
        binding.categoryContainer.removeAllViews()

        categories.forEach { value ->
            val button = android.widget.Button(this)

            button.text = value
            button.isAllCaps = false

            button.setOnClickListener {
                category = value
                favoritesOnly = false
                applyFilter()
            }

            binding.categoryContainer.addView(button)
        }
    }

    private fun setupAlphabet() {
        val alphabet = ('A'..'Z').toList()

        alphabet.forEach { c ->
            val button = android.widget.TextView(this)

            button.text = "$c  "
            button.textSize = 14f
            button.setPadding(4, 8, 4, 8)

            button.setOnClickListener {
                letter = c.toString()
                applyFilter()
            }

            binding.alphabetContainer.addView(button)
        }

        binding.alphabetContainer.setOnClickListener {
            letter = ""
            applyFilter()
        }
    }

    private fun setupActions() {
        binding.searchInput.setOnEditorActionListener { _, _, _ ->
            applyFilter()
            false
        }

        binding.searchInput.addTextChangedListener {
            applyFilter()
        }

        binding.btnFavorites.setOnClickListener {
            favoritesOnly = !favoritesOnly
            applyFilter()
        }

        binding.btnRefresh.setOnClickListener {
            refreshRemoteCatalog()
        }

        binding.btnDiagnostic.setOnClickListener {
            showDiagnostic()
        }

        binding.btnHistory.setOnClickListener {
            showHistory()
        }

        binding.btnSettings.setOnClickListener {
            showSettings()
        }
    }

    private fun loadCatalog() {
        when (val result = repository.loadCatalog()) {
            is CatalogResult.Success -> {
                allChannels = applyFavoriteState(result.channels)
                binding.textStatus.text =
                    "${allChannels.size} chaînes dans le catalogue"
                applyFilter()
            }

            is CatalogResult.Empty -> {
                allChannels = emptyList()
                adapter.submitList(emptyList())
                binding.textStatus.text = result.reason
            }

            is CatalogResult.Error -> {
                allChannels = emptyList()
                adapter.submitList(emptyList())
                binding.textStatus.text = result.message
            }
        }
    }

    private fun refreshRemoteCatalog() {
        binding.textStatus.text =
            "Actualisation du catalogue…"

        executor.execute {
            val result = repository.refreshFromInternet()

            runOnUiThread {
                when (result) {
                    is CatalogResult.Success -> {
                        allChannels =
                            applyFavoriteState(result.channels)

                        binding.textStatus.text =
                            "${allChannels.size} chaînes • catalogue actualisé"

                        applyFilter()
                    }

                    else -> {
                        binding.textStatus.text =
                            "Catalogue local conservé • Internet indisponible"
                    }
                }
            }
        }
    }

    private fun applyFavoriteState(
        channels: List<TvChannel>
    ): List<TvChannel> {
        val favorites = favoritesStore.load().tv

        return channels.map {
            it.copy(
                favorite = favorites.contains(it.id)
            )
        }
    }

    private fun applyFilter() {
        val query =
            binding.searchInput.text
                ?.toString()
                ?.trim()
                ?.lowercase()
                .orEmpty()

        var filtered = allChannels

        if (favoritesOnly) {
            filtered = filtered.filter { it.favorite }
        }

        if (category != "Toutes") {
            filtered = when (category) {
                "Français" ->
                    filtered.filter {
                        it.language.contains("fra", true) ||
                        it.language.contains("français", true) ||
                        it.language.contains("french", true)
                    }

                "Afrique" ->
                    filtered.filter {
                        it.country.equals("BJ", true) ||
                        it.country.equals("TG", true) ||
                        it.country.equals("CI", true) ||
                        it.country.equals("SN", true) ||
                        it.country.equals("CM", true) ||
                        it.country.equals("CD", true) ||
                        it.country.equals("GA", true) ||
                        it.country.equals("MA", true) ||
                        it.country.equals("DZ", true) ||
                        it.country.equals("TN", true)
                    }

                "Europe" ->
                    filtered.filter {
                        it.country in setOf(
                            "FR", "BE", "CH", "LU",
                            "DE", "IT", "ES", "PT",
                            "GB", "NL"
                        )
                    }

                "International" ->
                    filtered.filter {
                        it.country.isBlank()
                    }.ifEmpty {
                        filtered
                    }

                else ->
                    filtered.filter {
                        it.category.contains(category, true) ||
                        it.genre.contains(category, true)
                    }
            }
        }

        if (letter.isNotBlank()) {
            filtered = filtered.filter {
                it.name.trim()
                    .startsWith(letter, true)
            }
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.searchableText()
                    .contains(query)
            }
        }

        filtered = filtered.sortedBy {
            it.name.lowercase()
        }

        adapter.submitList(filtered)

        binding.textCount.text =
            "${filtered.size} résultat(s)"

        binding.textNoContent.visibility =
            if (filtered.isEmpty())
                View.VISIBLE
            else
                View.GONE
    }

    private fun openChannel(channel: TvChannel) {
        if (!channel.hasConfiguredStream()) {
            Toast.makeText(
                this,
                "Cette chaîne n'a aucun flux valide configuré.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        prefs.lastChannelId = channel.id
        prefs.pushHistory(channel.id)

        startActivity(
            Intent(this, PlayerActivity::class.java)
                .putExtra(
                    PlayerActivity.EXTRA_TITLE,
                    channel.name
                )
                .putExtra(
                    PlayerActivity.EXTRA_STREAM_URL,
                    channel.streamUrl
                )
                .putExtra(
                    PlayerActivity.EXTRA_ITEM_ID,
                    channel.id
                )
                .putExtra(
                    PlayerActivity.EXTRA_VOLUME_PERCENT,
                    prefs.volumePercent
                )
        )
    }

    private fun toggleFavorite(channel: TvChannel) {
        favoritesStore.toggleTvFavorite(channel.id)

        allChannels = applyFavoriteState(allChannels)

        applyFilter()
    }

    private fun showHistory() {
        val history = prefs.history()

        if (history.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Historique")
                .setMessage("Aucune chaîne récemment regardée.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val names = history.mapNotNull { id ->
            allChannels.firstOrNull { it.id == id }?.name
        }

        AlertDialog.Builder(this)
            .setTitle("Récemment regardées")
            .setItems(names.toTypedArray(), null)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSettings() {
        AlertDialog.Builder(this)
            .setTitle("Paramètres SCHAL-TV")
            .setItems(
                arrayOf(
                    "Thème sombre",
                    "Lecture automatique",
                    "Plein écran",
                    "Actualiser le catalogue",
                    "Effacer le cache"
                )
            ) { _, which ->
                when (which) {
                    3 -> refreshRemoteCatalog()

                    4 -> {
                        try {
                            cacheDir.deleteRecursively()
                            Toast.makeText(
                                this,
                                "Cache nettoyé.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (_: Exception) {
                        }
                    }
                }
            }
            .setNegativeButton("Fermer", null)
            .show()
    }

    private fun showDiagnostic() {
        val hasInternet =
            try {
                val cm =
                    getSystemService(CONNECTIVITY_SERVICE)
                            as android.net.ConnectivityManager

                cm.activeNetwork != null
            } catch (_: Exception) {
                false
            }

        val message = buildString {
            append("Internet : ")
            append(if (hasInternet) "OK" else "ERREUR")
            append("\n\n")

            append("Catalogue : ")
            append(if (allChannels.isNotEmpty()) "OK" else "ERREUR")
            append("\n")

            append("Chaînes : ")
            append(allChannels.size)
            append("\n")

            append("Favoris : ")
            append(favoritesStore.load().tv.size)
            append("\n")

            append("Historique : ")
            append(prefs.history().size)
            append("\n")

            append("\nSCHAL-TV • diagnostic local")
        }

        AlertDialog.Builder(this)
            .setTitle("Diagnostic")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onNavKey(key: NavKey): Boolean {
        return when (key) {
            NavKey.UP -> {
                adapter.moveSelection(-1)
                true
            }

            NavKey.DOWN -> {
                adapter.moveSelection(1)
                true
            }

            NavKey.OK -> {
                adapter.selectedChannel()?.let(::openChannel)
                true
            }

            NavKey.LEFT -> {
                adapter.moveSelection(-1)
                true
            }

            NavKey.RIGHT -> {
                adapter.moveSelection(1)
                true
            }

            NavKey.MENU -> {
                showDiagnostic()
                true
            }

            NavKey.BACK -> false
        }
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?
    ): Boolean {
        val key = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> NavKey.UP
            KeyEvent.KEYCODE_DPAD_DOWN -> NavKey.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> NavKey.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> NavKey.RIGHT
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> NavKey.OK
            KeyEvent.KEYCODE_MENU -> NavKey.MENU
            else -> null
        }

        if (key != null && onNavKey(key)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }
}

private fun android.widget.EditText.addTextChangedListener(
    callback: (CharSequence?) -> Unit
) {
    addTextChangedListener(
        object : android.text.TextWatcher {
            override fun afterTextChanged(
                s: android.text.Editable?
            ) {
                callback(s)
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
        }
    )
}
