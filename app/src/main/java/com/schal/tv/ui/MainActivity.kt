package com.schal.tv.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.schal.tv.R
import com.schal.tv.catalog.CatalogRepository
import com.schal.tv.core.CatalogResult
import com.schal.tv.core.NavKey
import com.schal.tv.core.NavigationController
import com.schal.tv.core.TvChannel
import com.schal.tv.databinding.ActivityMainBinding
import com.schal.tv.offline.LocalMediaScanner
import com.schal.tv.storage.FavoritesStore
import com.schal.tv.storage.Prefs

/**
 * Écran d'accueil : catalogue, recherche, favoris, infos, volume, chaîne
 * précédente/suivante, bouton lecture. Implémente NavigationController pour
 * que la future UI clavier physique (SCHAL BASIC) réutilise cette logique.
 */
class MainActivity : AppCompatActivity(), NavigationController {

    private lateinit var binding: ActivityMainBinding
    private lateinit var catalogRepository: CatalogRepository
    private lateinit var favoritesStore: FavoritesStore
    private lateinit var localMediaScanner: LocalMediaScanner
    private lateinit var prefs: Prefs

    private lateinit var adapter: ChannelAdapter
    private var fullChannelList: List<TvChannel> = emptyList()
    private var favoritesOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        catalogRepository = CatalogRepository(this)
        favoritesStore = FavoritesStore(this)
        localMediaScanner = LocalMediaScanner(this)
        prefs = Prefs(this)
        // SCHAL TV est volontairement OFFLINE-ONLY : le catalogue est embarqué
        // dans l'APK et aucune synchronisation réseau n'est effectuée.

        adapter = ChannelAdapter(
            onChannelSelected = { onChannelPicked(it) },
            onFavoriteToggle = { onFavoriteToggled(it) }
        )
        binding.recyclerChannels.layoutManager = LinearLayoutManager(this)
        binding.recyclerChannels.adapter = adapter

        binding.volumeSeekBar.progress = prefs.volumePercent
        binding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) prefs.volumePercent = progress
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.searchInput.addTextChangedListener(afterTextChanged = { applyFilter(it?.toString().orEmpty()) })

        binding.btnPlay.setOnClickListener { adapter.selectedChannel()?.let { onChannelPicked(it) } }
        binding.btnPrevChannel.setOnClickListener { moveSelection(-1) }
        binding.btnNextChannel.setOnClickListener { moveSelection(1) }
        binding.btnFavoritesFilter.setOnClickListener {
            favoritesOnly = !favoritesOnly
            binding.btnFavoritesFilter.isSelected = favoritesOnly
            applyFilter(binding.searchInput.text?.toString().orEmpty())
        }
        binding.btnInfo.setOnClickListener { showChannelInfo(adapter.selectedChannel()) }

        loadCatalog()
    }

    override fun onResume() {
        super.onResume()
        // Le mode offline/online peut avoir changé pendant que l'app était
        // en pause : on réévalue la bannière sans recharger tout le catalogue.
        updateConnectivityBanner()
    }

    private fun loadCatalog() {
        when (val result = catalogRepository.loadCatalog()) {
            is CatalogResult.Success -> {
                fullChannelList = applyFavoritesFlags(result.channels)
                applyFilter("")
                binding.textNoContent.visibility = android.view.View.GONE
            }
            is CatalogResult.Empty -> {
                fullChannelList = emptyList()
                adapter.submitList(emptyList())
                showNoContent(getString(R.string.no_offline_content))
            }
            is CatalogResult.Error -> {
                fullChannelList = emptyList()
                adapter.submitList(emptyList())
                showNoContent(result.message)
            }
        }
        updateConnectivityBanner()
    }

    private fun applyFavoritesFlags(channels: List<TvChannel>): List<TvChannel> {
        val favorites = favoritesStore.load().tv
        return channels.map { it.copy(favorite = favorites.contains(it.id)) }
    }

    private fun applyFilter(query: String) {
        var filtered = fullChannelList
        if (favoritesOnly) filtered = filtered.filter { it.favorite }
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }
        adapter.submitList(filtered)
        if (filtered.isEmpty() && fullChannelList.isNotEmpty()) {
            showNoContent(getString(R.string.no_results))
        } else if (filtered.isNotEmpty()) {
            binding.textNoContent.visibility = android.view.View.GONE
        }
    }

    private fun showNoContent(message: String) {
        binding.textNoContent.text = message
        binding.textNoContent.visibility = android.view.View.VISIBLE
    }

    private fun updateConnectivityBanner() {
        binding.textConnectivityBanner.text = getString(R.string.mode_offline)
    }

    private fun moveSelection(delta: Int) {
        adapter.moveSelection(delta)?.let {
            binding.recyclerChannels.scrollToPosition(adapter.currentList().indexOf(it))
        }
    }

    private fun onFavoriteToggled(channel: TvChannel) {
        val nowFavorite = favoritesStore.toggleTvFavorite(channel.id)
        fullChannelList = fullChannelList.map { if (it.id == channel.id) it.copy(favorite = nowFavorite) else it }
        applyFilter(binding.searchInput.text?.toString().orEmpty())
    }

    private fun onChannelPicked(channel: TvChannel) {
        if (!channel.hasConfiguredStream()) {
            Toast.makeText(this, R.string.stream_not_configured, Toast.LENGTH_SHORT).show()
            return
        }
        prefs.lastChannelId = channel.id
        prefs.pushHistory(channel.id)
        startActivity(
            Intent(this, PlayerActivity::class.java)
                .putExtra(PlayerActivity.EXTRA_TITLE, channel.name)
                .putExtra(PlayerActivity.EXTRA_STREAM_URL, channel.streamUrl)
                .putExtra(PlayerActivity.EXTRA_ITEM_ID, channel.id)
                .putExtra(PlayerActivity.EXTRA_VOLUME_PERCENT, prefs.volumePercent)
        )
    }

    private fun showChannelInfo(channel: TvChannel?) {
        if (channel == null) return
        val details = buildString {
            append(channel.name).append('\n')
            append(if (channel.country.isNotBlank()) channel.country else getString(R.string.unknown_field)).append('\n')
            append(if (channel.hasConfiguredStream()) channel.streamType.name else getString(R.string.stream_not_configured))
        }
        Toast.makeText(this, details, Toast.LENGTH_LONG).show()
    }

    // --- Navigation abstraite (D-pad / futur clavier physique) ---
    override fun onNavKey(key: NavKey): Boolean = when (key) {
        NavKey.UP -> { adapter.moveSelection(-1); true }
        NavKey.DOWN -> { adapter.moveSelection(1); true }
        NavKey.OK -> { adapter.selectedChannel()?.let { onChannelPicked(it) }; true }
        NavKey.LEFT -> { moveSelection(-1); true }
        NavKey.RIGHT -> { moveSelection(1); true }
        NavKey.MENU -> { showChannelInfo(adapter.selectedChannel()); true }
        NavKey.BACK -> false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val navKey = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> NavKey.UP
            KeyEvent.KEYCODE_DPAD_DOWN -> NavKey.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> NavKey.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> NavKey.RIGHT
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> NavKey.OK
            KeyEvent.KEYCODE_MENU -> NavKey.MENU
            else -> null
        }
        if (navKey != null && onNavKey(navKey)) return true
        return super.onKeyDown(keyCode, event)
    }
}

/** Petit utilitaire pour éviter d'ajouter une dépendance externe pour un simple TextWatcher. */
private fun android.widget.EditText.addTextChangedListener(afterTextChanged: (CharSequence?) -> Unit) {
    this.addTextChangedListener(object : android.text.TextWatcher {
        override fun afterTextChanged(s: android.text.Editable?) = afterTextChanged(s)
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}
