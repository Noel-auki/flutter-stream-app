import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'go_live.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Live Stream App',
      theme: ThemeData.dark(),
      home: MainPage(),
    );
  }
}

class MainPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Live Stream App'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            ElevatedButton(
              onPressed: () {
                // Navigator.push(
                //   context,
                //   MaterialPageRoute(builder: (context) => GoLivePage()),
                // );
                MethodChannel('com.example.ivs/broadcast')
                    .invokeMethod('golive');
              },
              child: Text('Go Live'),
            ),
            ElevatedButton(
              onPressed: () {
                // Navigate to Watch Live screen
                MethodChannel('com.example.ivs/broadcast')
                    .invokeMethod('watchLive');
              },
              child: Text('Watch Live'),
            ),
          ],
        ),
      ),
    );
  }
}
