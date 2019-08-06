package com.example.mediacodecasync;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    VideoViewPlayer videoViewPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoViewPlayer = findViewById(R.id.surfaceView);
        //videoViewPlayer.start();
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Maininit ","onResume to start");
        videoViewPlayer.start();
    }
}
