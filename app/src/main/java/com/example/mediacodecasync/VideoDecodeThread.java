package com.example.mediacodecasync;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecodeThread extends Thread {
    private Surface mSurface;
    private AssetFileDescriptor mAssetFileDescriptor;
    private final String TAG = "VideoDecodeThread";
    private MediaCodec videoCodec;
    private MediaExtractor mediaExtractor;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private MediaCodec.BufferInfo bufferInfo;
    private boolean isEOS = false;
    private long startMs;

    @Override
    public void run(){
        if(!init()){
            Log.e(TAG,"init wrong,return");
            return;
        }
        //int frameCount = 0;
        while (!Thread.interrupted()){
            if(!isEOS){
                int inputIndex = videoCodec.dequeueInputBuffer(0);
                if(inputIndex > 0){
                    ByteBuffer inputBuffer = inputBuffers[inputIndex];
                    int size = mediaExtractor.readSampleData(inputBuffer,0);
                    if(size < 0){
                        videoCodec.queueInputBuffer(inputIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEOS = true;
                        Log.d(TAG,"video input buffer is end");
                    }else{
                        long simpaletime = mediaExtractor.getSampleTime();
                        videoCodec.queueInputBuffer(inputIndex,0,size,simpaletime,0);
                    }
                    //准备下一个单位的数据
                    boolean advance = mediaExtractor.advance();
                    if (!advance) {
                        isEOS = false;
                    }
                }
            }

            int outputIndex = videoCodec.dequeueOutputBuffer(bufferInfo,0);
            switch (outputIndex){
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    MediaFormat mediaFormat = videoCodec.getOutputFormat();
                    Log.v(TAG, "format changed: "+mediaFormat);
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    //Log.d(TAG, "video time out");
                    break;
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(TAG,"buffer changed");
                default:
                    while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                        try{
                            sleep(5);
                        }catch (InterruptedException e){
                            Log.v(TAG,"output thread fail: "+ e.getMessage());
                        }
                    }
                   // Log.d(TAG,"frame count: "+frameCount++);
                    videoCodec.releaseOutputBuffer(outputIndex,true);
            }
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "buffer stream end");
                break;
            }
        }
        mediaExtractor.release();
        videoCodec.stop();
        videoCodec.release();
    }

    private boolean init(){
        mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(mAssetFileDescriptor.getFileDescriptor(),mAssetFileDescriptor.getStartOffset(),mAssetFileDescriptor.getLength());
        }catch (IOException e){
            Log.d(TAG,"setDataSource fail: "+e.getMessage());
        }
        int trackCount = mediaExtractor.getTrackCount();
        for(int i = 0; i != trackCount; ++i){
            MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
            String mimetype = mediaFormat.getString(MediaFormat.KEY_MIME);
            if(mimetype.startsWith("video/")){
                mediaExtractor.selectTrack(i);
                try{
                    videoCodec = MediaCodec.createDecoderByType(mimetype);
                }catch (IOException e){
                    Log.d(TAG,"create videoCodec fail: "+e.getMessage());
                    return false;
                }
                videoCodec.configure(mediaFormat,mSurface,null,0);
                break;
            }
        }
        if(videoCodec == null){
            Log.v("TAG","videoCodec constroctor lose");
            return false;
        }
        videoCodec.start();
        inputBuffers = videoCodec.getInputBuffers();
        outputBuffers = videoCodec.getOutputBuffers();
        bufferInfo = new MediaCodec.BufferInfo();
        startMs = System.currentTimeMillis();
        return true;
    }
    public VideoDecodeThread(Surface surface, AssetFileDescriptor assetFileDescriptor){
        mSurface = surface;
        mAssetFileDescriptor = assetFileDescriptor;
    }
}
