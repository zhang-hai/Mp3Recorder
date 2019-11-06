package com.buihha.audiorecorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Mp3Recorder {

    private static final String TAG = Mp3Recorder.class.getSimpleName();

    OnRecordListener mListener;

    static {
        System.loadLibrary("mp3lame");
    }

    private static final int DEFAULT_SAMPLING_RATE = 44100;
    private static final int DEFAULT_OUT_SAMPLING_RATE = 22050;

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

        return isRecording;

    }

    /**
     * Start recording. Create an encoding thread. Start record from this
     * thread.
     *
     * @throws IOException
     */
    public void startRecording(String dir, String name) throws IOException {
        if (isRecording) return;
        Log.d(TAG, "Start recording");
        Log.d(TAG, "BufferSize = " + bufferSize);
        // Initialize audioRecord if it's null.
        if (audioRecord == null) {

            File directory = new File(dir);
            if (!directory.exists()) {
                directory.mkdirs();
                Log.d(TAG, "Created directory");
            }

            mp3File = new File(directory, name);
            if(mp3File.exists()){//文件存在则删除
                mp3File.delete();
            }

            initAudioRecorder();
        }
        audioRecord.startRecording();
        if (mListener != null)
            mListener.onStart();

        new Thread() {

            @Override
            public void run() {
                isRecording = true;
                while (isRecording) {
                    //bytes是实际读取的数据长度，一般而言bytes会小于buffersize
                    int bytes = audioRecord.read(buffer, 0, bufferSize);
                    if (bytes > 0) {
                        long v = 0;
                        // 将 buffer 内容取出，进行平方和运算
                        for (int i = 0; i < buffer.length; i++) {
                            v += buffer[i] * buffer[i];
                        }
                        //平方和除以数据总长度，得到音量大小。
                        double mean = v / (double) bytes;
                        double volume = 10 * Math.log10(mean);
                        Log.d(TAG, "分贝值:" + volume);
                        if (mListener != null)
                            mListener.onRecording(audioRecord.getSampleRate(),volume);
                        ringBuffer.write(buffer, bytes);
                    }
                }

                // release and finalize audioRecord
                try {
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                    if (mListener != null)
                        mListener.onStop();

                    // stop the encoding thread and try to wait
                    // until the thread finishes its job
                    Message msg = Message.obtain(encodeThread.getHandler(),
                            DataEncodeThread.PROCESS_STOP);
                    msg.sendToTarget();

                    Log.d(TAG, "waiting for encoding thread");
                    encodeThread.join();
                    Log.d(TAG, "done encoding thread");
                } catch (InterruptedException e) {
                    Log.d(TAG, "Faile to join encode thread");
                } finally {
                    if (os != null) {
                        try {
                            os.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }.start();
    }

    /**
     *
     *
     */
    public void stopRecording() {
        Log.d(TAG, "stop recording");
        isRecording = false;
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
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRateInHz, channelConfig, audioFormat.getAudioFormat(),
                bufferSize);

        // Setup RingBuffer. Currently is 10 times size of hardware buffer
        // Initialize buffer to hold data
        ringBuffer = new RingBuffer(10 * bufferSize);
        buffer = new byte[bufferSize];

        // Initialize lame buffer
        // mp3 sampling rate is the same as the recorded pcm sampling rate
        // The bit rate is 32kbps
        // TODO:输入输出采样率一致时会有杂音，故此处将输入输出采样率设置不一样来解决噪音问题。
        //  必须都是双通道，还要就是查资料的时候说的是采样率必须一致，但此处相同采样率时，出现噪音问题，不知何原因
        SimpleLame.init(sampleRateInHz, 1, DEFAULT_OUT_SAMPLING_RATE, BIT_RATE);

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
    }

    public void setOnRecordListener(OnRecordListener listener) {
        this.mListener = listener;
    }
}