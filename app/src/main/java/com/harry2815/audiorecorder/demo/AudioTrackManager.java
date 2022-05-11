package com.harry2815.audiorecorder.demo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;


/**
 * 实时播放录音的PCM数据管理器
 */
public class AudioTrackManager {

    private AudioTrack audioTrack;

    private byte[] data;

    private volatile boolean playing = true;

    //开始
    public void start(){
        audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 16000, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 1280, AudioTrack.MODE_STREAM);
        audioTrack.play();
        this.playing = true;
        new Thread(){
            @Override
            public void run() {
                while (playing){
                    if (data != null && data.length > 0){
                        audioTrack.write(data,0,data.length);
                        data = null;
                    }
                }
            }
        }.start();
    }

    //
    public void write(byte[] data){
        this.data = data;
    }

    //停止
    public void stopPlay(){
        this.playing = false;
        if (audioTrack != null){
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }

}
