package com.schal.tv.ui

import android.os.Bundle
import android.view.KeyEvent
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.schal.tv.core.NavKey
import com.schal.tv.core.NavigationController
import com.schal.tv.databinding.ActivityPlayerBinding
import com.schal.tv.player.PlaybackState
import com.schal.tv.player.PlayerListener
import com.schal.tv.player.PlayerManager
import com.schal.tv.storage.Prefs

class PlayerActivity : AppCompatActivity(), NavigationController, PlayerListener {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var playerManager: PlayerManager
    private lateinit var prefs: Prefs
    private var itemId: String = ""
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        playerManager = PlayerManager(this)
        playerManager.setListener(this)
        playerManager.attachView(binding.playerView)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL).orEmpty()
        itemId = intent.getStringExtra(EXTRA_ITEM_ID).orEmpty()
        val volumePercent = intent.getIntExtra(EXTRA_VOLUME_PERCENT, prefs.volumePercent)

        binding.channelTitle.text = title
        binding.volumeSeekBar.progress = volumePercent
        playerManager.setVolumePercent(volumePercent)

        binding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    playerManager.setVolumePercent(progress)
                    prefs.volumePercent = progress
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnPlayPause.setOnClickListener { togglePlayPause() }
        binding.btnBack.setOnClickListener { finish() }

        val resumeMs = if (itemId.isNotBlank()) prefs.resumePositionMs(itemId) else 0L
        if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
            showStatus("Cette chaîne en direct nécessite Internet. Le catalogue SCHAL TV reste disponible hors ligne.")
        } else {
            playerManager.play(streamUrl, resumeMs)
        }
    }

    private fun togglePlayPause() {
        if (isPlaying) playerManager.pause() else playerManager.resume()
    }

    override fun onStateChanged(state: PlaybackState) {
        runOnUiThread {
            when (state) {
                is PlaybackState.Playing -> {
                    isPlaying = true
                    binding.textPlayerStatus.visibility = android.view.View.GONE
                }
                is PlaybackState.Paused -> isPlaying = false
                is PlaybackState.Buffering -> showStatus("Chargement…")
                is PlaybackState.Ended -> showStatus("Lecture terminée")
                is PlaybackState.Error -> {
                    isPlaying = false
                    showStatus(state.message)
                }
                PlaybackState.Idle -> {}
            }
        }
    }

    private fun showStatus(message: String) {
        binding.textPlayerStatus.text = message
        binding.textPlayerStatus.visibility = android.view.View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        if (itemId.isNotBlank()) {
            prefs.setResumePositionMs(itemId, playerManager.currentPositionMs())
        }
        playerManager.pause()
    }

    override fun onDestroy() {
        playerManager.release()
        super.onDestroy()
    }

    override fun onNavKey(key: NavKey): Boolean = when (key) {
        NavKey.OK -> { togglePlayPause(); true }
        NavKey.BACK -> { finish(); true }
        NavKey.UP -> { adjustVolume(+5); true }
        NavKey.DOWN -> { adjustVolume(-5); true }
        else -> false
    }

    private fun adjustVolume(delta: Int) {
        val newValue = (binding.volumeSeekBar.progress + delta).coerceIn(0, 100)
        binding.volumeSeekBar.progress = newValue
        playerManager.setVolumePercent(newValue)
        prefs.volumePercent = newValue
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val navKey = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> NavKey.OK
            KeyEvent.KEYCODE_BACK -> NavKey.BACK
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_VOLUME_UP -> NavKey.UP
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN -> NavKey.DOWN
            else -> null
        }
        if (navKey != null && onNavKey(navKey)) return true
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_STREAM_URL = "extra_stream_url"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_VOLUME_PERCENT = "extra_volume_percent"
    }
}
