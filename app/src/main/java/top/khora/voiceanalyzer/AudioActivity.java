package top.khora.voiceanalyzer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class AudioActivity extends AppCompatActivity implements View.OnClickListener {
    private static String TAG="AudioActivity";
    private Button btn_open;
    private Button btn_stop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAuth();
        initial();
        initMinBufferSize();
        initAudioRecord();
    }
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    public void checkAuth(){
        Log.i(TAG,"---checkAuth---");
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(AudioActivity.this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG,"---checkAuth---未授权录音，开始询问是否授权录音");
            ActivityCompat.requestPermissions(AudioActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);//根据第三个参数对授权后回调
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG,"---onRequestPermissionsResult---授权录音成功回调，开始初始化AudioRecord");
                    initial();
                    initMinBufferSize();
                    initAudioRecord();
                } else {
                    Log.i(TAG,"---onRequestPermissionsResult---授权录音失败，准备提示用户并关闭应用");
                    Toast toast=Toast.makeText(this,null,Toast.LENGTH_SHORT);
                    toast.setText("录音未授权，无法正常使用该应用");
                    toast.show();
                    //-TODO 延迟关闭应用
                }

            }
        }

    }
    private void initial(){
        btn_open = findViewById(R.id.audio_act_btn_open);
        btn_stop = findViewById(R.id.audio_act_btn_stop);
        btn_open.setOnClickListener(this);
        btn_stop.setOnClickListener(this);
    }

    /**
     * 一、初始化获取每一帧流的Size
     * */
    private Integer mRecordBufferSize;
    private void initMinBufferSize(){
        Log.i(TAG,"---initMinBufferSize---");
        //获取每一帧的字节流大小
        /**
         * 1.sampleRateInHz 采样率，4000~192000范围内
         * 在AudioFormat类里
         * public static final int SAMPLE_RATE_HZ_MIN = 4000; 最小4000
         * public static final int SAMPLE_RATE_HZ_MAX = 192000; 最大192000
         * 2.声道配置 描述音频声道的配置,例如左声道/右声道/前声道/后声道。
         * public static final int CHANNEL_IN_LEFT = 0x4;//左声道
         * public static final int CHANNEL_IN_RIGHT = 0x8;//右声道
         * public static final int CHANNEL_IN_FRONT = 0x10;//前声道
         * public static final int CHANNEL_IN_BACK = 0x20;//后声道
         * public static final int CHANNEL_IN_LEFT_PROCESSED = 0x40;
         * public static final int CHANNEL_IN_RIGHT_PROCESSED = 0x80;
         * public static final int CHANNEL_IN_FRONT_PROCESSED = 0x100;
         * public static final int CHANNEL_IN_BACK_PROCESSED = 0x200;
         * public static final int CHANNEL_IN_PRESSURE = 0x400;
         * public static final int CHANNEL_IN_X_AXIS = 0x800;
         * public static final int CHANNEL_IN_Y_AXIS = 0x1000;
         * public static final int CHANNEL_IN_Z_AXIS = 0x2000;
         * public static final int CHANNEL_IN_VOICE_UPLINK = 0x4000;
         * public static final int CHANNEL_IN_VOICE_DNLINK = 0x8000;
         * public static final int CHANNEL_IN_MONO = CHANNEL_IN_FRONT;//单声道
         * public static final int CHANNEL_IN_STEREO = (CHANNEL_IN_LEFT | CHANNEL_IN_RIGHT);//立体声道(左右声道)
         *3.音频格式 表示音频数据的格式。
         * public static final int ENCODING_PCM_16BIT = 2; //16位PCM编码
         * public static final int ENCODING_PCM_8BIT = 3; //8位PCM编码
         * public static final int ENCODING_PCM_FLOAT = 4; //4位PCM编码
         * public static final int ENCODING_AC3 = 5;
         * public static final int ENCODING_E_AC3 = 6;
         * public static final int ENCODING_DTS = 7;
         * public static final int ENCODING_DTS_HD = 8;
         * public static final int ENCODING_MP3 = 9; //MP3编码 此格式可能会因为不设备不支持报错
         * public static final int ENCODING_AAC_LC = 10;
         * public static final int ENCODING_AAC_HE_V1 = 11;
         * public static final int ENCODING_AAC_HE_V2 = 12;
         * */
        mRecordBufferSize = AudioRecord.getMinBufferSize(8000
                , AudioFormat.CHANNEL_IN_MONO
                , AudioFormat.ENCODING_PCM_16BIT);
    }
    /**
     * 二、初始化音频录制AudioRecord
     * */
    private AudioRecord mAudioRecord;
    private void initAudioRecord(){
        Log.i(TAG,"---initAudioRecord---");
        /**
         * 第一个参数audioSource 音频源   这里选择使用麦克风：MediaRecorder.AudioSource.MIC
         * 第二个参数sampleRateInHz 采样率（赫兹）  与前面初始化获取每一帧流的Size保持一致
         * 第三个参数channelConfig 声道配置 描述音频声道的配置,例如左声道/右声道/前声道/后声道。   与前面初始化获取每一帧流的Size保持一致
         * 第四个参数audioFormat 音频格式  表示音频数据的格式。  与前面初始化获取每一帧流的Size保持一致
         * 第五个参数缓存区大小,就是上面我们配置的AudioRecord.getMinBufferSize
         * */
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC
                , 8000
                , AudioFormat.CHANNEL_IN_MONO
                , AudioFormat.ENCODING_PCM_16BIT
                , mRecordBufferSize);
    }
    /**
     * 三、开始录制与保存录制音频文件
     * */
    private boolean mWhetherRecord;
    private File pcmFile;
    private String fname;
    private void startRecord(){
        Log.i(TAG,"---startRecord---");
        fname= String.valueOf(new Date().getTime());
        pcmFile = new File(AudioActivity.this.getExternalCacheDir().getPath(),"audioRecord"+fname+".pcm");
        mWhetherRecord = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();//开始录制
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(pcmFile);
                    byte[] bytes = new byte[mRecordBufferSize];
                    while (mWhetherRecord){
                        mAudioRecord.read(bytes, 0, bytes.length);//读取流
                        fileOutputStream.write(bytes);
                        fileOutputStream.flush();
                    }
                    Log.e(TAG, "run: 暂停录制" );
                    mAudioRecord.stop();//停止录制
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    addHeadData();//添加音频头部信息并且转成wav格式
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    mAudioRecord.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }
    /**
     * 停止录制
     * */
    private void stopRecord(){
        Log.i(TAG,"---stopRecord---");
        mWhetherRecord = false;
    }
    /**
     * 给音频文件添加头部信息,并且转换格式成wav
     * */
    private void addHeadData(){
        Log.i(TAG,"---addHeadData---");
        pcmFile = new File(AudioActivity.this.getExternalCacheDir().getPath(),"audioRecord"+fname+".pcm");
        File handlerWavFile = new File(AudioActivity.this.getExternalCacheDir().getPath(),"audioRecord_handler"+fname+".wav");
        PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(8000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
        pcmToWavUtil.pcmToWav(pcmFile.toString(),handlerWavFile.toString());
    }
    /**
     * 释放AudioRecord,录制流程完毕
     * 之后可在目录找到音频文件
     * */
    private void releaseAR(){
        Log.i(TAG,"---releaseAR---");
        mAudioRecord.release();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.audio_act_btn_open:
                Log.i(TAG,"开始录音");
                initAudioRecord();
                startRecord();
                break;
            case R.id.audio_act_btn_stop:
                Log.i(TAG,"停止录音");
                stopRecord();
                releaseAR();
                break;

        }
    }
}
