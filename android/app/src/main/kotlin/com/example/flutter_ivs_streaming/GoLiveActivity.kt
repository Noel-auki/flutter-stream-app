package com.example.flutter_ivs_streaming
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amazonaws.ivs.broadcast.*
import com.example.flutter_ivs_streaming.MainActivity.Companion
import java.util.Calendar

class GoLiveActivity : AppCompatActivity() {
    private lateinit var startStreamButton: Button
    private lateinit var switchCameraButton: Button
    private lateinit var broadcastSession: BroadcastSession
    private lateinit var currentCamera: Device
    private var isStreaming = false

    private val broadcastListener = object : BroadcastSession.Listener() {
        override fun onStateChanged(state: BroadcastSession.State) {
            Log.d(TAG, "State=$state")
        }

        override fun onError(exception: BroadcastException) {
            Log.e(TAG, "Exception: $exception")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_go_live)

        startStreamButton = findViewById(R.id.startStreamButton)
        switchCameraButton = findViewById(R.id.switchCameraButton)

        if (allPermissionsGranted()) {
            setupBroadcastSession()
        } else {
            requestPermissions()
        }

        startStreamButton.setOnClickListener {
            if (isStreaming) {
                stopStreaming()
            } else {
                startStreaming()
            }
        }

        switchCameraButton.setOnClickListener {
            swapCamera()
        }
    }

    private fun setupBroadcastSession() {
        val context = applicationContext
        broadcastSession = BroadcastSession(
            context,
            broadcastListener,
            Presets.Configuration.STANDARD_PORTRAIT,
            null
        )

        broadcastSession.awaitDeviceChanges {
            for (desc in BroadcastSession.listAvailableDevices(context)) {
                if (desc.type == Device.Descriptor.DeviceType.CAMERA && desc.position == Device.Descriptor.Position.FRONT) {
                    broadcastSession.attachDevice(desc) { device ->
                        val previewHolder = findViewById<LinearLayout>(R.id.previewHolder)
                        val preview = (device as ImageDevice).previewView
                        preview.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        previewHolder.addView(preview)
                        currentCamera = device
                    }
                    break
                }
            }
        }
    }

    private fun startStreaming() {
        val streamKey = "sk_us-east-1_EHzA24d29E5F_RSdCke127kxiEFc5iNlXofzzIkWFRL"
        val rtmpsUrl = "rtmps://3893e27cd44d.global-contribute.live-video.net:443/app/"
        broadcastSession.start(rtmpsUrl, streamKey)
        sendTimestampMetadata()
        startStreamButton.text = "Stop Stream"
        isStreaming = true
    }

    private fun stopStreaming() {
        broadcastSession.stop()
        startStreamButton.text = "Start Stream"
        isStreaming = false
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
            Log.d(MainActivity.TAG, "Sent metadata: $metadata")
        } else {
            Log.e(MainActivity.TAG, "Failed to send metadata: $metadata")
        }
    }
    private fun swapCamera() {
        for (desc in BroadcastSession.listAvailableDevices(applicationContext)) {
            if (desc.type == Device.Descriptor.DeviceType.CAMERA && desc.position != currentCamera.descriptor.position) {
                broadcastSession.exchangeDevices(currentCamera, desc) { newCamera ->
                    val previewHolder = findViewById<LinearLayout>(R.id.previewHolder)
                    previewHolder.removeAllViews()
                    val preview = (newCamera as ImageDevice).previewView
                    preview.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    previewHolder.addView(preview)
                    currentCamera = newCamera
                }
                break
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                setupBroadcastSession()
            } else {
                // Permission not granted. Show a message to the user explaining why the permission is necessary.
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        findViewById<LinearLayout>(R.id.previewHolder).removeAllViews()
        broadcastSession.release()
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val TAG = "GoLiveActivity"
    }
}