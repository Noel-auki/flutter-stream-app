import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

class GoLivePage extends StatefulWidget {
  @override
  _GoLivePageState createState() => _GoLivePageState();
}

class _GoLivePageState extends State<GoLivePage> {
  static const platform = MethodChannel('com.example.ivs/broadcast');
  bool _isStreaming = false; // Track the streaming state

  @override
  void initState() {
    super.initState();
    _requestPermissions();
  }

  Future<void> _requestPermissions() async {
    final statuses = await [Permission.camera, Permission.microphone].request();
    if (statuses[Permission.camera]!.isGranted &&
        statuses[Permission.microphone]!.isGranted) {
      // Permissions granted
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
            content: Text(
                'Camera and Microphone permissions are required to go live')),
      );
    }
  }

  Future<void> _startStream() async {
    try {
      final result = await platform.invokeMethod('startStream', {
        'ingestEndpoint': '3893e27cd44d.global-contribute.live-video.net',
        'streamKey': 'sk_us-east-1_EHzA24d29E5F_RSdCke127kxiEFc5iNlXofzzIkWFRL',
      });
      print('Stream started: $result');
      setState(() {
        _isStreaming = true;
      });
    } on PlatformException catch (e) {
      print('Failed to start stream: ${e.message}');
    }
  }

  Future<void> _stopStream() async {
    try {
      final result = await platform.invokeMethod('stopStream');
      print('Stream stopped: $result');
      setState(() {
        _isStreaming = false;
      });
    } on PlatformException catch (e) {
      print('Failed to stop stream: ${e.message}');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Go Live'),
      ),
      body: Column(
        children: [
          Expanded(
            flex: 7,
            child: AndroidView(
              viewType: 'broadcast_preview',
            ),
          ),
          SizedBox(height: 10),
          ElevatedButton(
            onPressed: _isStreaming ? _stopStream : _startStream,
            child: Text(_isStreaming ? 'Stop Stream' : 'Start Stream'),
          ),
          SizedBox(height: 10),
        ],
      ),
    );
  }

  @override
  void dispose() {
    super.dispose();
  }
}
