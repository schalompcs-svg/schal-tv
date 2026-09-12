package com.schal.tv.ui

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.schal.tv.catalog.CatalogRepository
import com.schal.tv.core.CatalogResult
import com.schal.tv.databinding.ActivityPlayerBinding
import com.schal.tv.player.PlaybackState
import com.schal.tv.player.PlayerListener
import com.schal.tv.player.PlayerManager
import com.schal.tv.storage.Prefs

class PlayerActivity :
    AppCompatActivity(),
    PlayerListener {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var playerManager: PlayerManager
    private lateinit var prefs: Prefs

    private var itemId = ""
    private var currentTitle = ""

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_STREAM_URL = "stream_url"
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_VOLUME_PERCENT = "volume_percent"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)

        playerManager = PlayerManager(this)
        playerManager.setListener(this)
        playerManager.attachView(binding.playerView)

        currentTitle =
            intent.getStringExtra(EXTRA_TITLE).orEmpty()

        val streamUrl =
            intent.getStringExtra(EXTRA_STREAM_URL).orEmpty()

        itemId =
            intent.getStringExtra(EXTRA_ITEM_ID).orEmpty()

        binding.channelTitle.text = currentTitle

        binding.volumeSeekBar.progress =
            intent.getIntExtra(
                EXTRA_VOLUME_PERCENT,
                prefs.volumePercent
            )

        playerManager.setVolumePercent(
            binding.volumeSeekBar.progress
        )

        binding.volumeSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        playerManager.setVolumePercent(progress)
                        prefs.volumePercent = progress
                    }
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }
            }
        )

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnFullscreen.setOnClickListener {
            toggleFullscreen()
        }

        binding.btnPlayPause.setOnClickListener {
            if (binding.btnPlayPause.text
                    .toString()
                    .contains("Lecture", true)
            ) {
                playerManager.resume()
            } else {
                playerManager.pause()
            }
        }

        setupOtherChannels()

        if (
            streamUrl.startsWith("http://") ||
            streamUrl.startsWith("https://")
        ) {
            playerManager.play(streamUrl)
        } else {
            onStateChanged(
                PlaybackState.Error(
                    "Flux Internet non configuré."
                )
            )
        }
    }

    private fun setupOtherChannels() {
        val repository = CatalogRepository(this)

        when (val result = repository.loadCatalog()) {
            is CatalogResult.Success -> {

                val others =
                    result.channels
                        .filter { it.id != itemId }
                        .sortedBy { it.name.lowercase() }

                val adapter = ChannelAdapter(
                    onChannelSelected = { channel ->
                        prefs.lastChannelId = channel.id
                        prefs.pushHistory(channel.id)

                        finish()

                        startActivity(
                            intent
                                .putExtra(
                                    EXTRA_TITLE,
                                    channel.name
                                )
                                .putExtra(
                                    EXTRA_STREAM_URL,
                                    channel.streamUrl
                                )
                                .putExtra(
                                    EXTRA_ITEM_ID,
                                    channel.id
                                )
                        )
                    },
                    onFavoriteToggle = {
                        // Les favoris restent gérés depuis l'écran principal.
                    }
                )

                adapter.submitList(others)

                binding.recyclerOtherChannels.layoutManager =
                    LinearLayoutManager(this)

                binding.recyclerOtherChannels.adapter =
                    adapter
            }

            else -> {
                binding.textOtherStatus.text =
                    "Autres chaînes indisponibles hors connexion."
            }
        }
    }

    private fun toggleFullscreen() {
        if (requestedOrientation ==
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        ) {
            requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_SENSOR

            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_VISIBLE
        } else {
            requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    override fun onStateChanged(
        state: PlaybackState
    ) {
        runOnUiThread {
            when (state) {
                PlaybackState.Buffering -> {
                    binding.textPlayerStatus.visibility =
                        View.VISIBLE

                    binding.textPlayerStatus.text =
                        "Chargement du flux…"
                }

                PlaybackState.Playing -> {
                    binding.textPlayerStatus.visibility =
                        View.GONE

                    binding.btnPlayPause.text =
                        "Pause"
                }

                PlaybackState.Paused -> {
                    binding.textPlayerStatus.visibility =
                        View.VISIBLE

                    binding.textPlayerStatus.text =
                        "Lecture en pause"

                    binding.btnPlayPause.text =
                        "Lecture"
                }

                is PlaybackState.Error -> {
                    binding.textPlayerStatus.visibility =
                        View.VISIBLE

                    binding.textPlayerStatus.text =
                        state.message

                    binding.btnPlayPause.text =
                        "Lecture"
                }

                else -> {
                    binding.textPlayerStatus.visibility =
                        View.GONE
                }
            }
        }
    }

    override fun onPause() {
        if (itemId.isNotBlank()) {
            prefs.setResumePositionMs(
                itemId,
                playerManager.currentPositionMs()
            )
        }

        super.onPause()
    }

    override fun onDestroy() {
        playerManager.release()
        super.onDestroy()
    }
}
