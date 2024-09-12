package com.example.flutter_ivs_streaming


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amazonaws.ivs.broadcast.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException


class GoLiveActivity : AppCompatActivity() {
    private lateinit var startStreamButton: Button
    private lateinit var switchCameraButton: Button
    private lateinit var urlInput: EditText
    private lateinit var broadcastSession: BroadcastSession
    private lateinit var currentCamera: Device
    private var microphoneDevice: Device? = null
    private var isStreaming = false
    private val handler = Handler(Looper.getMainLooper())
    // Preload the default URL into the EditText
    private val urlTemplate = "http://%s:5000/capture-timestamp"

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
        urlInput = findViewById(R.id.urlInput)


        // Set the default value in the input field
        val defaultIp = "ec2-54-86-156-117.compute-1.amazonaws.com"
        urlInput.setText(defaultIp)

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


        // Creating the broadcast session
        broadcastSession = BroadcastSession(
            context,
            broadcastListener,
            Presets.Configuration.STANDARD_PORTRAIT,
            null
        )


        broadcastSession.awaitDeviceChanges {
            var cameraAttached = false


            for (desc in BroadcastSession.listAvailableDevices(context)) {
                when (desc.type) {
                    Device.Descriptor.DeviceType.CAMERA -> {
                        if (!cameraAttached && desc.position == Device.Descriptor.Position.FRONT) {
                            broadcastSession.attachDevice(desc) { device ->
                                val previewHolder = findViewById<LinearLayout>(R.id.previewHolder)
                                val preview = (device as ImageDevice).previewView
                                preview.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.MATCH_PARENT
                                )
                                previewHolder.addView(preview)
                                currentCamera = device
                                cameraAttached = true
                            }
                        }
                    }
                    Device.Descriptor.DeviceType.MICROPHONE -> {
                        // Explicitly attaching the microphone device
                        broadcastSession.attachDevice(desc) { device ->
                            microphoneDevice = device
                            Log.d(TAG, "Microphone attached")
                        }
                    }
                    else -> {}
                }
            }


            if (!cameraAttached) {
                Log.e(TAG, "Failed to attach camera device")
            }
            if (microphoneDevice == null) {
                Log.e(TAG, "Failed to attach microphone device")
            }
        }
    }


    private fun startStreaming() {
        val streamKey = "sk_us-east-1_EHzA24d29E5F_RSdCke127kxiEFc5iNlXofzzIkWFRL"
        val rtmpsUrl = "rtmps://3893e27cd44d.global-contribute.live-video.net:443/app/"
        broadcastSession.start(rtmpsUrl, streamKey)
        startStreamButton.text = "Stop Stream"
        isStreaming = true


        // Capture current timestamp and send to server
        val currentTimestamp = System.currentTimeMillis()
        sendTimestampToServer(currentTimestamp)
    }


    private fun sendTimestampToServer(timestamp: Long) {
        val ip = urlInput.text.toString()
        if (ip.isEmpty()) {
            Log.e(TAG, "Server URL is empty")
            return
        }

        // Construct the full URL by replacing the placeholder with the user-provided IP
        val url = String.format(urlTemplate, ip)

        val json = JSONObject().apply {
            put("type", "video")
            put("timestamp", timestamp)
            put("playback_url", "https://3893e27cd44d.us-east-1.playback.live-video.net/api/video/v1/us-east-1.007088424812.channel.5MNTJLnhzuZJ.m3u8")
        }

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url(url)  // Use the URL constructed from the template and user input
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to send data to server: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d(TAG, "Successfully sent data to server")
            }
        })
    }

    private fun stopStreaming() {
        broadcastSession.stop()
        startStreamButton.text = "Start Stream"
        isStreaming = false
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
