package com.example.mediacodecasync;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class VideoViewPlayer extends SurfaceView implements SurfaceHolder.Callback {
    private final String TAG = "VideoViewPlayer";
    private boolean isCreated = false;
    private VideoDecodeThread videoDecodeThread;
    private AudioDecodeThread audioDecodeThread;
    private Context mContext;
    private AssetFileDescriptor assetFileDescriptor;

    public VideoViewPlayer(Context context){
        super(context);
        mContext = context;
        getHolder().addCallback(this);
        getFileDescriptor();

    }
    public VideoViewPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        getHolder().addCallback(this);
        getFileDescriptor();
    }

    public VideoViewPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        getHolder().addCallback(this);
        getFileDescriptor();
    }

    public void getFileDescriptor(){
        try {
            AssetManager assetManager = mContext.getAssets();
            assetFileDescriptor = assetManager.openFd("video.mp4");
        }catch (IOException e){
            Log.d(TAG, e.getMessage());
        }
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder){
        Log.d(TAG,"surface created videoView");
        isCreated = true;
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder,int format, int width, int heigth){
        Log.d(TAG,"surface changed～～");
//
//        if(videoDecodeThread == null){
//            videoDecodeThread = new VideoDecodeThread(holder.getSurface(),assetFileDescriptor);
//        }
//        if(audioDecodeThread == null){
//            audioDecodeThread = new AudioDecodeThread(assetFileDescriptor);
//        }
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        isCreated = false;
        if(videoDecodeThread != null){
            videoDecodeThread.interrupt();
        }
        if(audioDecodeThread != null){
            audioDecodeThread.interrupt();
        }
        Log.d(TAG,"surface destroyed");
    }

    public void start(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(!isCreated){
                    try {
                        Thread.sleep(10);
                        Log.d(TAG,"sleep wait created.");
                    }catch (InterruptedException e){
                        Log.d(TAG,"thread interrupted: "+e.getMessage());
                    }
                }
                if(isCreated){
                    Log.d(TAG,"start!");
                    if(videoDecodeThread == null){
                        videoDecodeThread = new VideoDecodeThread(getHolder().getSurface(),assetFileDescriptor);
                    }
                    videoDecodeThread.start();
                    if(audioDecodeThread == null){
                        if(assetFileDescriptor == null){
                            Log.d(TAG,"file is null for audio");
                        }
                        audioDecodeThread = new AudioDecodeThread(assetFileDescriptor);
                    }
                    audioDecodeThread.start();
                }
            }
        }).start();
    }
}
