package com.example.mediacodecasync;

import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioDecodeThread extends Thread {
    private final String TAG = "AudioDecodeThread";
    private AssetFileDescriptor mAssetFileDescriptor;

    private MediaCodec audioCodec;
    private MediaExtractor mediaExtractor;
    private AudioPlayer audioPlayer;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private MediaCodec.BufferInfo bufferInfo;
    private long startMs;
    private boolean isEOS = false;

    @Override
    public void run(){
        if(!init()){
            Log.e(TAG,"init wrong,return");
            return;
        }
        int audioFrame = 0;
        while (!Thread.interrupted()){
            if(!isEOS){
                int inputIndex = audioCodec.dequeueInputBuffer(0);
                if(inputIndex > 0){
                    ByteBuffer inputBuffer = inputBuffers[inputIndex];
                    int size = mediaExtractor.readSampleData(inputBuffer,0);
                    if(size < 0){
                        audioCodec.queueInputBuffer(inputIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEOS = true;
                        Log.d(TAG,"video input buffer is end");
                    }else{
                        long simpaletime = mediaExtractor.getSampleTime();
                        audioCodec.queueInputBuffer(inputIndex,0,size,simpaletime,0);
                    }
                    //准备下一个单位的数据
                    boolean advance = mediaExtractor.advance();
                    if (!advance) {
                        isEOS = false;
                    }
                }
            }

            int outIndex = audioCodec.dequeueOutputBuffer(bufferInfo, 0);
            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(TAG,"buffer changed");
                    outputBuffers = audioCodec.getOutputBuffers();
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    MediaFormat mediaFormat = audioCodec.getOutputFormat();
                    Log.v(TAG, "format changed: "+mediaFormat);
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.d(TAG, "dequeueOutputBuffer timed out!");
                    break;
                default:
                    Log.d(TAG,"audio frame count: "+audioFrame++);
                    ByteBuffer buffer = outputBuffers[outIndex];

                    while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    //用来保存解码后的数据
                    byte[] outData = new byte[bufferInfo.size];
                    buffer.get(outData);
                    //清空缓存
                    buffer.clear();
                    //播放解码后的数据
                    audioPlayer.play(outData, 0, bufferInfo.size);
                    audioCodec.releaseOutputBuffer(outIndex, false);
                    break;
            }

            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "buffer stream end");
                break;
            }

        }
        mediaExtractor.release();
        audioCodec.stop();
        audioCodec.release();
    }

    public boolean init(){
        mediaExtractor = new MediaExtractor();
        try{
            if(mAssetFileDescriptor == null){
                Log.d(TAG,"file is null in audio");
            }
            mediaExtractor.setDataSource(mAssetFileDescriptor.getFileDescriptor(),mAssetFileDescriptor.getStartOffset(),mAssetFileDescriptor.getLength());
        }catch (IOException e){
            Log.v(TAG,"audio setDataSource fail: "+e.getMessage());
        }

        int trackCount =mediaExtractor.getTrackCount();
        Log.d(TAG,"trackCount is: "+trackCount);
        for(int i = 0; i != trackCount; ++i){
            MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
            String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
            if(mimeType.startsWith("audio/")){
                mediaExtractor.selectTrack(i);
                try{
                    Log.d(TAG,"mimeType is: "+mimeType);
                    audioCodec = MediaCodec.createDecoderByType(mimeType);
                }catch (IOException e){
                    Log.v(TAG,"create audioCodec fail: "+e.getMessage());
                    return false;
                }
                audioCodec.configure(mediaFormat,null,null,0);
                int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                int channels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                audioPlayer = new AudioPlayer(sampleRate,(channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO), AudioFormat.ENCODING_PCM_16BIT);
                break;
            }
        }
        if(audioCodec == null){
            Log.v(TAG,"audioCodec contrustor lose");
            return false;
        }
        audioCodec.start();
        inputBuffers = audioCodec.getInputBuffers();
        outputBuffers = audioCodec.getOutputBuffers();
        bufferInfo = new MediaCodec.BufferInfo();
        startMs = System.currentTimeMillis();
        return true;
    }

    public AudioDecodeThread(AssetFileDescriptor assetFileDescriptor){
        mAssetFileDescriptor = assetFileDescriptor;
    }
}
