package com.example.flutter_ivs_streaming
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.ivs.player.*
import java.nio.ByteBuffer
import java.util.*

class WatchLiveActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_live)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerView = findViewById(R.id.playerView)

        // Load Uri to play
        playerView.player.load(Uri.parse("https://937a5aa6b93e.us-west-2.playback.live-video.net/api/video/v1/us-west-2.730335574547.channel.3lcLwrlSljEj.m3u8"))


        // Set Listener for Player callback events
        handlePlayerEvents()
    }

    override fun onStart() {
        super.onStart()
        playerView.player.play()
    }

    override fun onStop() {
        super.onStop()
        playerView.player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerView.player.release()
    }

    private fun updateQuality() {
        val qualitySpinner = findViewById<Spinner>(R.id.quality_spinner)
        var auto = "auto"
        val currentQuality: String = playerView.player.quality.name
        if (playerView.player.isAutoQualityMode && !TextUtils.isEmpty(currentQuality)) {
            auto += " ($currentQuality)"
        }
        var selected = 0
        val names: ArrayList<String?> = ArrayList()
        for (quality in playerView.player.qualities) {
            names.add(quality.name)
        }
        names.add(0, auto)
        if (!playerView.player.isAutoQualityMode) {
            for (i in names.indices) {
                if (names[i].equals(currentQuality)) {
                    selected = i
                }
            }
        }
        val qualityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        qualitySpinner.onItemSelectedListener = null
        qualitySpinner.adapter = qualityAdapter
        qualitySpinner.setSelection(selected, false)
        qualitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val name = qualityAdapter.getItem(position)
                if (name != null && name.startsWith("auto")) {
                    playerView.player.isAutoQualityMode = true
                } else {
                    for (quality in playerView.player.qualities) {
                        if (quality.name.equals(name, ignoreCase = true)) {
                            Log.i("IVSPlayer", "Quality Selected: $quality")
                            playerView.player.setQuality(quality)
                            break
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * Demonstration for what callback APIs are available to listen for Player events.
     */
    private fun handlePlayerEvents() {
        playerView.player.apply {
            // Listen to changes on the player
            addListener(object : Player.Listener() {
                override fun onAnalyticsEvent(p0: String, p1: String) {}
                override fun onDurationChanged(p0: Long) {
                    // If the video is a VOD, you can seek to a duration in the video
                    Log.i("IVSPlayer", "New duration: $duration")
                    seekTo(p0)
                }
                override fun onError(p0: PlayerException) {}
                override fun onMetadata(type: String, data: ByteBuffer) {}
                override fun onQualityChanged(p0: Quality) {
                    Log.i("IVSPlayer", "Quality changed to $p0")
                    updateQuality()
                }
                override fun onRebuffering() {}
                override fun onSeekCompleted(p0: Long) {}
                override fun onVideoSizeChanged(p0: Int, p1: Int) {}
                override fun onCue(cue: Cue) {
                    when (cue) {
                        is TextMetadataCue -> Log.i("IVSPlayer", "Received Text Metadata: ${cue.text}")
                    }
                }

                override fun onStateChanged(state: Player.State) {
                    Log.i("PlayerLog", "Current state: $state")
                    when (state) {
                        Player.State.BUFFERING,
                        Player.State.READY -> {
                            updateQuality()
                        }
                        Player.State.IDLE,
                        Player.State.ENDED -> {
                            // no-op
                        }
                        Player.State.PLAYING -> {
                            // Qualities will be dependent on the video loaded, and can
                            // be retrieved from the player
                            Log.i("IVSPlayer", "Available Qualities: ${qualities}")
                        }
                    }
                }
            })
        }
    }
}
