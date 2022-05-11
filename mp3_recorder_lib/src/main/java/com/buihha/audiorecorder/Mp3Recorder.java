package com.buihha.audiorecorder;

import android.util.Log;

import com.buihha.audiorecorder.other.RecordConfig;
import com.buihha.audiorecorder.other.RecordHelper;
import com.buihha.audiorecorder.other.listener.RecordDataListener;
import com.buihha.audiorecorder.other.listener.RecordStateListener;

import java.io.File;
import java.io.IOException;

public class Mp3Recorder {

    private static final String TAG = Mp3Recorder.class.getSimpleName();

    private OnRecordListener mListener;
    private RecordDataListener recordDataListener;

    public File mp3File;

    private RecordConfig config;

    private boolean isRecording = false;

    /**
     *
     * @param config 参数配置
     */
    public Mp3Recorder(RecordConfig config) {
        this.config = config;
    }



    /**
     * Default constructor. Setup recorder with default sampling rate 1 channel,
     * 16 bits pcm
     */
    public Mp3Recorder() {
        this(RecordConfig.getDefaultConfig());
    }

    public boolean isRecording() {
        isRecording = RecordHelper.getInstance().isRecording();
        return isRecording;
    }

    /**
     * Start recording. Create an encoding thread. Start record from this
     * thread.
     *
     * @throws IOException
     */
    public void startRecording(String dir, String name) throws IOException {
        mp3File = new File(dir == null ? "":dir, name == null ? "":name);
        RecordHelper.getInstance().setRecordStateListener(new RecordStateListener() {
            @Override
            public void onStateChange(RecordHelper.RecordState state) {
                if (mListener != null){
                    if(state == RecordHelper.RecordState.RECORDING){
                        mListener.onStart();
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.d("Mp3Recorder","onError----->"+error);
                isRecording = false;
                if (mListener != null){
                    mListener.onError();
                }
            }
        });
        RecordHelper.getInstance().setRecordResultListener(result -> {
            //结束录音
            if(result == null) return;
            mp3File = result;
            if(mListener != null){
                mListener.onStop();
            }
        });
        RecordHelper.getInstance().setRecordSoundSizeListener(soundSize -> {
            if(mListener != null){
                mListener.onRecording(config.getSampleRate(),soundSize);
            }
        });
        RecordHelper.getInstance().setRecordDataListener(recordDataListener);
        config.setRecordDir(dir);
        RecordHelper.getInstance().start(mp3File.getAbsolutePath(),config);
    }

    /**
     *
     *
     */
    public void stopRecording() {
        Log.d(TAG, "stop recording");
        isRecording = false;
        RecordHelper.getInstance().stop();
    }

    public interface OnRecordListener {
        void onStart();

        void onStop();

        void onRecording(int sampleRate,double volume);//采样率和音量（分贝）

        void onError();
    }

    public void setOnRecordListener(OnRecordListener listener) {
        this.mListener = listener;
    }

    /**
     * 录音pcm数据监听,通过监听该回调可获取实时录音的PCM数据
     * 注：无论是否录制MP3都会返回PCM数据
     * @param listener 回调
     */
    public void setOnRecordDataListener(RecordDataListener listener){
        this.recordDataListener = listener;
    }
}