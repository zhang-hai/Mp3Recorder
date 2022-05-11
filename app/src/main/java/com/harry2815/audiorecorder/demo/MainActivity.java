package com.harry2815.audiorecorder.demo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import com.buihha.audiorecorder.Mp3Recorder;
import com.buihha.audiorecorder.other.RecordConfig;
import com.buihha.audiorecorder.other.listener.RecordDataListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhanghai on 2019/6/22.
 * function：
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_recorder;
    private TextView tv_filepath;
    private Button btn_play;

    private boolean isRecording = false;//正在录音
    private boolean isPlaying = false;//正在播放

    private Mp3Recorder mRecorder;

    private MediaPlayer mediaPlayer;

    private String mMp3Path;

    private AudioTrackManager audioTrackManager;

    private final String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_layout);

        initView();

        requestPermission();
    }

    private void initView(){
        btn_play = findViewById(R.id.btn_play);
        btn_recorder = findViewById(R.id.btn_recorder);
        tv_filepath = findViewById(R.id.tv_filepath);

        btn_recorder.setOnClickListener(this);
        btn_play.setOnClickListener(this);

        audioTrackManager = new AudioTrackManager();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_recorder){
            if(!isPlaying){//没在播放
                if(!isRecording){//开启录音
                    startRecord();

//                    audioTrackManager.start();

                    btn_recorder.setText("停止录音");
                }else {
                    stopRecord();

//                    audioTrackManager.stopPlay();

                    btn_recorder.setText("开始录音");
                }
                isRecording = !isRecording;
            }
        }else if(v.getId() == R.id.btn_play){
            if(!isRecording){
                if(!isPlaying){
                    if (TextUtils.isEmpty(mMp3Path) || !new File(mMp3Path).exists()){
                        Toast.makeText(this,"文件不存在",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startPlay();
                    btn_play.setText("停止播放");
                }else {
                    stopPlay();
                    btn_play.setText("开始播放");
                }
                isPlaying = !isPlaying;
            }
        }
    }

    //开始录音
    private void startRecord(){
        if (mRecorder == null) {
            //方式一：需要自定义配置，可使用该方式
            RecordConfig config = RecordConfig.getDefaultConfig();
            mRecorder = new Mp3Recorder(config);
            //方法二：采用默认配置
            //mRecorder = new Mp3Recorder();
            mRecorder.setOnRecordListener(new Mp3Recorder.OnRecordListener() {
                @Override
                public void onStart() {
                    //开始录音
                    Log.d("MainActivity","开始录音--------->>>");
                }

                @Override
                public void onError() {
                    Log.d("MainActivity","录音--------->>>onError");
                    btn_recorder.setText("开始录音");
                    isRecording = false;
                }

                @Override
                public void onStop() {
                    //停止录音
                    mMp3Path = mRecorder.mp3File.getAbsolutePath();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_filepath.setText(mMp3Path);
                        }
                    });
                }

                @Override
                public void onRecording(int i, double v) {
                    Log.d("MainActivity","采样:"+i+"Hz   音量:"+v+"分贝");
                }
            });

            //新增 监听录音PCM数据，需要时可以注册该监听器（这里做了获取到数据采用AudioTrack进行实时播放的示例）
            mRecorder.setOnRecordDataListener(new RecordDataListener() {
                @Override
                public void onData(byte[] data) {
                    Log.d("MainActivity","实时数据长度：" + data.length);
//                    audioTrackManager.write(data);
                }
            });
        }
        if (!mRecorder.isRecording()){
            try {
                mRecorder.startRecording(getExternalFilesDir("").getAbsolutePath(), "record.mp3");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //停止录音
    private void stopRecord(){
        if (mRecorder!=null && mRecorder.isRecording()){
            mRecorder.stopRecording();
        }
    }

    //开始播放
    private void startPlay(){
        if(mediaPlayer == null){
            mediaPlayer = new MediaPlayer();
        }
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(mMp3Path);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    isPlaying = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btn_play.setText("开始播放");
                        }
                    });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //停止播放
    private void stopPlay(){
        if(mediaPlayer != null && mediaPlayer.isPlaying()){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    //检查是否有相应权限
    private boolean checkPermission(){
        return selfPermissionGranted(Manifest.permission.RECORD_AUDIO);
    }

    /**
     * api级别权限查询
     * @param permission
     * @return
     */
    private boolean selfPermissionGranted(String permission) {
        return  PermissionChecker.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED;
    }

    /**
     *
     * 麦克风	RECORD_AUDIO  危险	麦克风的使用
     * @return
     */
    public boolean checkAudioPermission(){
        try {
            AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            // 无用操作
            audioManager.getMode();
        }catch (Exception e){
            return false;
        }
        return selfPermissionGranted(Manifest.permission.RECORD_AUDIO);
    }

    //申请权限
    private void requestPermission(){
        if(Build.VERSION.SDK_INT >= 23){
            if(!checkAudioPermission()){
                List<String> needRequestPermissionList = new ArrayList<>();
                for (String permission : PERMISSIONS) {
                    if (PermissionChecker.checkSelfPermission(this, permission) != PermissionChecker.PERMISSION_GRANTED
                            || ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        needRequestPermissionList.add(permission);
                    }
                }
                if(needRequestPermissionList.size() > 0){
                    ActivityCompat.requestPermissions(this,needRequestPermissionList.toArray(new String[0]),100);
                }
            }
        }
    }

    /**
     * 确认所有的权限是否都已授权
     * @param grantResults
     * @return
     */
    private boolean verifyPermissions(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (verifyPermissions(grantResults)) {
                //
            } else {
                finish();
            }
        }
    }
}
