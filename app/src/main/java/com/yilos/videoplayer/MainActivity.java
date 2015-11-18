package com.yilos.videoplayer;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        VideoPlayerView videoView = (VideoPlayerView) findViewById(R.id.video_player);
        videoView.setUriAddress("http://v.yilos.com/51f40a4693a9a6256e93b7f18e34facf.mp4");
//        videoView.playVideo();
    }
}