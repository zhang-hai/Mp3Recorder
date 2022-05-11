# Mp3Recorder
录音mp3格式（lame 采用边录边转码方式）

窃来的，好几个项目都用，很方便，提取出来形成引用包

#### 引用

##### Project build.gradle配置

```groovy
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

##### Module build.gradel配置

```groovy
dependencies {
        implementation 'com.github.zhang-hai:Mp3Recorder:1.0.11'
}
```

#### 使用

```xml
//需要先声明权限
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
//注：存储权限需根据自身应用存储位置来判断是否添加
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```



**开始录音**

```java
//动态申请权限（略）
if (mRecorder == null) {
	mRecorder = new Mp3Recorder();
	mRecorder.setOnRecordListener(new Mp3Recorder.OnRecordListener() {
		@Override
		public void onStart() {
			//开始录音
		}

		@Override
		public void onStop() {
			//停止录音
		}

		@Override
		public void onError(){
		    //录音错误，主要针对OPPO手机在调用startRecord方法时弹窗安全权限提示，此时如果拒绝，则会执行该回调

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
            Log.d("MainActivity","实时PCM数据长度：" + data.length);
        }
    });
}
if (!mRecorder.isRecording())
	try {
		mRecorder.startRecording("/sdcard","record.mp3");
	} catch (IOException e) {
		e.printStackTrace();
	}
```

注：`Mp3Recorder`类提供了配置录音参数的构造函数，建议采用默认录音参数。`若无需保存录音文件可通过RecordConfig.setSaveToFile(boolean saveToFile)方法设置，默认是保存`

**停止录音**

```java
if (mRecorder!=null && mRecorder.isRecording()){
	mRecorder.stopRecording();
}
```

#### 版本更新
v1.0.5 设置默认音频采样率未44100Hz,因为Android要求所有设备都要支持该采样率，其他采样率不一定。

v1.0.7

v1.0.8 修改初始化AudioRecord对象是参数AudioSource类型，解决录音文件有噪音的问题;

v1.0.9 修复转码读取pcm数据未对short转化成byte进行转换，造成出现音频中偶尔刺啦声问题；

v1.0.10 开启录音时，提前把mp3File对象构造出来

v1.0.11 

- 新增`setOnRecordDataListener(RecordDataListener listener)`用于监听实时录音的PCM数据；
- 在`RecordConfig`中新增`saveToFile`变量，用于标识是否保存录音文件，默认true；



