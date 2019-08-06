package com.example.mediacodecasync;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioPlayer {
    private final String TAG = "AudioPlayer";
    private int mSampleRate;
    private int mChannels;
    private int mSampleBits;
    private AudioTrack audioTrack;

    public AudioPlayer(int sampleRate, int channels, int sampleBits){
        mSampleRate = sampleRate;
        mChannels = channels;
        mSampleBits = sampleBits;
        init();
    }

    public void init(){
        if(audioTrack != null){
            release();
        }
        int minBufferSize = AudioTrack.getMinBufferSize(mSampleRate,mChannels,mSampleBits);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate, mChannels, mSampleBits, minBufferSize, AudioTrack.MODE_STREAM);
        audioTrack.play();
    }

    private void release(){
        if(audioTrack != null){
            audioTrack.stop();
            audioTrack.release();
        }
    }

    public void play(byte[] data,int offset, int length){
        if(data == null || length ==0){
            return;
        }
        try{
            audioTrack.write(data,offset,length);
        }catch (Exception e){
            Log.v(TAG,"audioTrack write wrong: "+ e.getMessage());
        }
    }
}
