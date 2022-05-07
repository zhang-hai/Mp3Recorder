package com.buihha.audiorecorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.buihha.audiorecorder.other.RecordConfig;
import com.buihha.audiorecorder.other.RecordHelper;
import com.buihha.audiorecorder.other.listener.RecordResultListener;
import com.buihha.audiorecorder.other.listener.RecordSoundSizeListener;
import com.buihha.audiorecorder.other.listener.RecordStateListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Mp3Recorder {

    private static final String TAG = Mp3Recorder.class.getSimpleName();

    OnRecordListener mListener;

    private static final int DEFAULT_SAMPLING_RATE = 16000;

    private static final int FRAME_COUNT = 160;

    /* Encoded bit rate. MP3 file will be encoded with bit rate 32kbps */
    private static final int BIT_RATE = 32;

    private AudioRecord audioRecord = null;

    private int bufferSize;

    public File mp3File;

    private RingBuffer ringBuffer;

    private byte[] buffer;

    private FileOutputStream os = null;

    private DataEncodeThread encodeThread;

    private int sampleRateInHz;

    private int channelConfig;

    private PCMFormat audioFormat;

    private boolean isRecording = false;

    /**
     *
     * @param sampleRateInHz the sample rate expressed in Hertz. 44100Hz is currently the only
      *   rate that is guaranteed to work on all devices, but other rates such as 22050,
      *   16000, and 11025 may work on some devices.
      *   {@link AudioFormat#SAMPLE_RATE_UNSPECIFIED} means to use a route-dependent value
      *   which is usually the sample rate of the source.
      *   {@link AudioRecord#getSampleRate()} can be used to retrieve the actual sample rate chosen.
      * @param channelConfig describes the configuration of the audio channels.
     *   See {@link AudioFormat#CHANNEL_IN_MONO} and
     *   {@link AudioFormat#CHANNEL_IN_STEREO}.  {@link AudioFormat#CHANNEL_IN_MONO} is guaranteed
     *   to work on all devices.
     * @param audioFormat the format in which the audio data is to be returned.
     *   See {@link AudioFormat#ENCODING_PCM_8BIT}, {@link AudioFormat#ENCODING_PCM_16BIT},
     *   and {@link AudioFormat#ENCODING_PCM_FLOAT}.
     */
    public Mp3Recorder(int sampleRateInHz, int channelConfig,
                       PCMFormat audioFormat) {
        this.sampleRateInHz = sampleRateInHz;
        this.channelConfig = channelConfig;
        this.audioFormat = audioFormat;
    }

    /**
     * Default constructor. Setup recorder with default sampling rate 1 channel,
     * 16 bits pcm
     */
    public Mp3Recorder() {
        this(DEFAULT_SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO,
                PCMFormat.PCM_16BIT);
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
        mp3File = new File(dir, name);
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
        RecordHelper.getInstance().setRecordResultListener(new RecordResultListener() {
            @Override
            public void onResult(File result) {
                //结束录音
                if(result == null) return;
                mp3File = result;
                if(mListener != null){
                    mListener.onStop();
                }
            }
        });
        final RecordConfig config = new RecordConfig();
        RecordHelper.getInstance().setRecordSoundSizeListener(new RecordSoundSizeListener() {
            @Override
            public void onSoundSize(int soundSize) {
                if(mListener != null){
                    mListener.onRecording(config.getSampleRate(),soundSize);
                }
            }
        });

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

    /**
     * Initialize audio recorder
     */
    private void initAudioRecorder() throws IOException {
        int bytesPerFrame = audioFormat.getBytesPerFrame();
		/* Get number of samples. Calculate the buffer size (round up to the
		   factor of given frame size) */
        int frameSize = AudioRecord.getMinBufferSize(sampleRateInHz,
                channelConfig, audioFormat.getAudioFormat()) / bytesPerFrame;
        if (frameSize % FRAME_COUNT != 0) {
            frameSize = frameSize + (FRAME_COUNT - frameSize % FRAME_COUNT);
            Log.d(TAG, "Frame size: " + frameSize);
        }

        bufferSize = frameSize * bytesPerFrame;

        /* Setup audio recorder */
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRateInHz, channelConfig, audioFormat.getAudioFormat(),
                bufferSize);

        // Setup RingBuffer. Currently is 10 times size of hardware buffer
        // Initialize buffer to hold data
        ringBuffer = new RingBuffer(10 * bufferSize);
        buffer = new byte[bufferSize];

        // Initialize lame buffer
        // mp3 sampling rate is the same as the recorded pcm sampling rate
        // The bit rate is 32kbps
        SimpleLame.init(sampleRateInHz, 1, sampleRateInHz, BIT_RATE);

        // Initialize the place to put mp3 file
//		String externalPath = Environment.getExternalStorageDirectory()
//				.getAbsolutePath();

        //开启追加
        os = new FileOutputStream(mp3File, true);

        // Create and run thread used to encode data
        // The thread will
        encodeThread = new DataEncodeThread(ringBuffer, os, bufferSize);
        encodeThread.start();
        audioRecord.setRecordPositionUpdateListener(encodeThread, encodeThread.getHandler());
        audioRecord.setPositionNotificationPeriod(FRAME_COUNT);
    }

    /**
     * 检查设备是否支持指定的音频采样率
     */
    private void invalidSampleRates() {
        for (int rate : new int[] {8000, 11025, 16000, 22050, 44100}) {  // add the rates you wish to check against
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_CONFIGURATION_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                // buffer size is valid, Sample rate supported
            }
        }
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
}