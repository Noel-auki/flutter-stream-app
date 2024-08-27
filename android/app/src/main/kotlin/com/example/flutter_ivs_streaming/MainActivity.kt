package com.example.flutter_ivs_streaming

import android.content.Intent
import android.util.Log
import com.amazonaws.ivs.broadcast.BroadcastException
import com.amazonaws.ivs.broadcast.BroadcastSession
import com.amazonaws.ivs.broadcast.Device
import com.amazonaws.ivs.broadcast.Presets
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.util.Calendar

class MainActivity : FlutterActivity() {
    private var broadcastSession: BroadcastSession? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "startStream" -> {
                        val ingestEndpoint = call.argument<String>("ingestEndpoint")
                        val streamKey = call.argument<String>("streamKey")
                        startBroadcastSession(ingestEndpoint, streamKey)
                        result.success("Stream started")
                    }
                    "golive" -> {
                        val intent = Intent(this, GoLiveActivity::class.java)
                        startActivity(intent)
                        result.success("Navigating to Go Live")
                    }
                    "stopStream" -> {
                        stopBroadcastSession()
                        result.success("Stream stopped")
                    }
                    "watchLive" -> {
                        val intent = Intent(this, WatchLiveActivity::class.java)
                        startActivity(intent)
                        result.success("Navigating to Watch Live")
                    }
                    else -> result.notImplemented()
                }
            }
    }

    private fun startBroadcastSession(ingestEndpoint: String?, streamKey: String?) {
        val broadcastListener: BroadcastSession.Listener = object : BroadcastSession.Listener() {
            override fun onStateChanged(state: BroadcastSession.State) {
                Log.d(TAG, "State=$state")
            }

            override fun onError(exception: BroadcastException) {
                Log.e(TAG, "Exception: $exception")
            }
        }

        try {
            broadcastSession = BroadcastSession(
                applicationContext,
                broadcastListener,
                Presets.Configuration.STANDARD_PORTRAIT,
                Presets.Devices.FRONT_CAMERA(applicationContext)
            )

            broadcastSession!!.awaitDeviceChanges {
                for (device in broadcastSession!!.listAttachedDevices()) {
                    if (device.descriptor.type === Device.Descriptor.DeviceType.CAMERA) {
                        // The preview can be set up here if needed
                    }
                }
            }

            val url = "rtmps://$ingestEndpoint/app"
            broadcastSession!!.start(url, streamKey)

            // Send timestamp as timed metadata
            sendTimestampMetadata()

        } catch (e: BroadcastException) {
            Log.e(TAG, "Error starting broadcast session", e)
        }
    }

    private fun sendTimestampMetadata() {
        val calendar = Calendar.getInstance()
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)
        val milliseconds = calendar.get(Calendar.MILLISECOND)

        val timestamp = String.format(
            "%02d:%02d:%02d.%03d",
            hours,
            minutes,
            seconds,
            milliseconds
        )

        val metadata = "timestamp:$timestamp"
        val success = broadcastSession?.sendTimedMetadata(metadata)
        if (success == true) {
            Log.d(TAG, "Sent metadata: $metadata")
        } else {
            Log.e(TAG, "Failed to send metadata: $metadata")
        }
    }



    private fun stopBroadcastSession() {
        if (broadcastSession != null) {
            broadcastSession!!.stop()
            broadcastSession!!.release()
            broadcastSession = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBroadcastSession()
    }

    companion object {
        private const val CHANNEL = "com.example.ivs/broadcast"
        const val TAG = "IVSBroadcast"
    }
}
