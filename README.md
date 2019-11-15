# Mp3Recorder
录音mp3格式（lame 采用边录边转码方式）

窃来的，忘记几年前从哪里找到的代码，好几个项目都用，很方便，提取出来形成引用包

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
        implementation 'com.github.zhang-hai:Mp3Recorder:1.0.8'
}
```

#### 使用

```xml
//需要先声明权限
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```



开始录音

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
                public void onRecording(int i, double v) {
                        Log.d("MainActivity","采样:"+i+"Hz   音量:"+v+"分贝");
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



停止录音

```java
if (mRecorder!=null && mRecorder.isRecording()){
	mRecorder.stopRecording();
}
```

#### 版本更新
v1.0.5 设置默认音频采样率未44100Hz,因为Android要求所有设备都要支持该采样率，其他采样率不一定。

v1.0.7

v1.0.8 修改初始化AudioRecord对象是参数AudioSource类型，解决录音文件有噪音的问题
