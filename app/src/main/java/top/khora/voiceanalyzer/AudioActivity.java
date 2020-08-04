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
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import top.khora.voiceanalyzer.Util.FFT;

public class AudioActivity extends AppCompatActivity implements View.OnClickListener {
    private static String TAG="AudioActivity";
    public static String path;
    private Button btn_open;
    private Button btn_stop;
    private Button btn_fre;
    private Button btn_fft;
    private Button btn_analy;
    private Button btn_set;
    private LineChart chart_fft;
    private LineChart chart_voice;

    public static int sampleRate=8192;
    public static int fftNum=4096;//根据fft方法的原理需要为2的整数幂
    //sampleRate/(fftNum/2)为fft后的频谱图的频率的区间单位,因为两个比特转为一个double

    public static int fftNumOfDrawPoints=1000;//最多用前一半，即不可超过（fftNum/2）/2
    private LinearLayout linearLayout1;
    private LinearLayout linearLayout2;
    private LinearLayout linearLayout3;
    private LinearLayout linearLayout4;
    private static final int PAGE_1=1;
    private static final int PAGE_2=2;
    private static final int PAGE_3=3;
    private static final int PAGE_4=4;
    private int pageNow=1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        path=getExternalFilesDir("").getPath();
        checkAuth();

        initial_before();
        initial1();
        initial_after();

        initMinBufferSize();
        initAudioRecord();
        startRecord();

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
                    initial_before();
                    initial1();
//                    initial2();
//                    initial3();
//                    initial4();
                    initial_after();

                    initMinBufferSize();
                    initAudioRecord();
                    startRecord();
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
    /**
     * 页面初始化
     * */
    private void initial_before(){
        btn_open = findViewById(R.id.audio_act_btn_open);
        btn_stop = findViewById(R.id.audio_act_btn_stop);
        btn_fre = findViewById(R.id.audio_act_btn_page_fre);
        btn_fft = findViewById(R.id.audio_act_btn_page_fft);
        btn_analy = findViewById(R.id.audio_act_btn_page_anlay);
        btn_set = findViewById(R.id.audio_act_btn_page_set);
        linearLayout1 = findViewById(R.id.linearlayout_1);
        linearLayout2 = findViewById(R.id.linearlayout_2);
        linearLayout3 = findViewById(R.id.linearlayout_3);
        linearLayout4 = findViewById(R.id.linearlayout_4);
    }
    private void switchLayout(int linearLayoutNum){//目前由initial 1~4方法调用
        Log.e(TAG,"切换内布局到："+linearLayoutNum);
        switch (linearLayoutNum){
            case 1:
                linearLayout1.setVisibility(View.VISIBLE);
                linearLayout2.setVisibility(View.GONE);
                linearLayout3.setVisibility(View.GONE);
                linearLayout4.setVisibility(View.GONE);
                pageNow=PAGE_1;
                break;
            case 2:
                linearLayout1.setVisibility(View.GONE);
                linearLayout2.setVisibility(View.VISIBLE);
                linearLayout3.setVisibility(View.GONE);
                linearLayout4.setVisibility(View.GONE);
                pageNow=PAGE_2;
                break;
            case 3:
                linearLayout1.setVisibility(View.GONE);
                linearLayout2.setVisibility(View.GONE);
                linearLayout3.setVisibility(View.VISIBLE);
                linearLayout4.setVisibility(View.GONE);
                pageNow=PAGE_3;
                break;
            case 4:
                linearLayout1.setVisibility(View.GONE);
                linearLayout2.setVisibility(View.GONE);
                linearLayout3.setVisibility(View.GONE);
                linearLayout4.setVisibility(View.VISIBLE);
                pageNow=PAGE_4;
                break;

        }
    }
    private void initial1(){
        switchLayout(1);
        chart_voice = findViewById(R.id.chart_voice);
    }
    private void initial2(){
        switchLayout(2);
        chart_fft = findViewById(R.id.chart_fft);
    }
    private void initial3(){
        switchLayout(3);

    }
    private void initial4(){
        switchLayout(4);

    }
    private void initial_after(){
        btn_open.setOnClickListener(this);
        btn_stop.setOnClickListener(this);
        btn_fre.setOnClickListener(this);
        btn_fft.setOnClickListener(this);
        btn_analy.setOnClickListener(this);
        btn_set.setOnClickListener(this);
    }
    /**
     * 图表更新
     * */
    protected void updateChartFre(HashMap hashMap){
        if (chart_fft==null){
            Log.e(TAG, "updateChartFre: 未找到所需的chart_fft对象");
            return;
        }
        List<Entry> entries=new ArrayList<>();
        Iterator it=hashMap.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry HMentry = (Map.Entry) it.next();
            Entry entry=new Entry(Float.valueOf(String.valueOf(HMentry.getKey()))
                    ,Float.valueOf(String.valueOf(HMentry.getValue())));
            entries.add(entry);
        }
        LineDataSet dataSet = new LineDataSet(entries, "Label"); // add entries to dataset
        dataSet.setColor(R.color.testChart1);
        dataSet.setValueTextColor(R.color.testChart1); // styling, ...
        LineData lineData = new LineData(dataSet);
        chart_fft.setData(lineData);
        XAxis xAxis= chart_fft.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMaximum(2500);
        xAxis.setAxisMinimum(20);

        YAxis yAxis= chart_fft.getAxisLeft();
        yAxis.setAxisMaximum(120);
        yAxis.setAxisMinimum(0);
        YAxis yAxis2= chart_fft.getAxisRight();
        yAxis2.setAxisMaximum(120);
        yAxis2.setAxisMinimum(0);

        chart_fft.invalidate(); // refresh
//        if (mWhetherRecord) {
//            audioHandler.postDelayed(fftRunnable,1);
//        }
    }
    protected void updateChartVoice(Deque deque){
        if (chart_voice==null){
            Log.e(TAG, "updateChartFre: 未找到所需的chart_voice对象");
            return;
        }
        List<Entry> entries=new ArrayList<>();
        List<Float> queueData=new ArrayList<>(deque);
//        System.out.println(queueData.toString());
        for (int i=0;i<queueData.size();++i){
            Entry entry=new Entry(i,(Float) queueData.get(i));
            entries.add(entry);
        }

        LineDataSet dataSet = new LineDataSet(entries, "Label"); // add entries to dataset
        dataSet.setColor(R.color.testChart1);
        dataSet.setValueTextColor(R.color.testChart1); // styling, ...
        LineData lineData = new LineData(dataSet);
        chart_voice.setData(lineData);

        YAxis yAxis= chart_voice.getAxisLeft();
        yAxis.setAxisMaximum(500);
        yAxis.setAxisMinimum(0);
        YAxis yAxis2= chart_voice.getAxisRight();
        yAxis2.setAxisMaximum(500);
        yAxis2.setAxisMinimum(0);

        chart_voice.invalidate(); // refresh
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
        mRecordBufferSize = AudioRecord.getMinBufferSize(sampleRate
                , AudioFormat.CHANNEL_IN_MONO
                , AudioFormat.ENCODING_PCM_16BIT);//8000，单声道，16位：默认取到最小缓冲为640byte，计算方法暂时不知道
        System.out.println("最小缓冲："+mRecordBufferSize);
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
                , sampleRate
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
//    byte[] bytes = new byte[fftNum];
    short[] shorts = new short[fftNum/2];

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
//                    audioHandler.post(fftRunnable);
                    fileOutputStream = new FileOutputStream(pcmFile);
//                    bytes = new byte[fftNum];//16位即2比特一个数值，所以fft计算点为该大小/2
                    shorts=new short[fftNum/2];
                    HashMap<Double,Double> hmAllFre;
                    ArrayDeque<Float> VoiceFreDeque=new ArrayDeque<>(80);
                    for (int i=0;i<80;++i){
                        VoiceFreDeque.push((float) 0);
                    }
                    while (mWhetherRecord){
                        mAudioRecord.read(shorts, 0, shorts.length);//读取流
//
                        Log.e("BYTES--LENGTH",shorts.length+"");
                        List resList=FFT.fft(shorts);
                        double maxFre= (double) resList.get(0);
                        hmAllFre= (HashMap<Double, Double>) resList.get(1);
                        Log.e(TAG,"hm大小"+hmAllFre.size());
                        if (maxFre>=49 && maxFre<=500) {//过滤
                            Log.e(TAG,"最大响度的频率："+maxFre);
                            if (VoiceFreDeque.size()>=80) {
                                VoiceFreDeque.pop();
                            }
                            VoiceFreDeque.add((float) maxFre);
                        }else {
                            if (VoiceFreDeque.size()>=80) {
                                VoiceFreDeque.pop();
                            }
                            VoiceFreDeque.add((float) 0);
                        }

                        if (pageNow==PAGE_1) {
                            updateChartVoice(VoiceFreDeque);//时间-主频率图
                        }

                        if (pageNow==PAGE_2) {
                            updateChartFre(hmAllFre);//某次fft的频谱图
                        }

//                        while (count*128<bytes.length) {
//                            FFT.fft(Arrays.copyOfRange(bytes,count*128,(count+1)*128));
//                            count++;
//                        }
                        fileOutputStream.write(short2byte(shorts));
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
        PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(sampleRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
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

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    /**
     * 暂时未用
     * */
    Runnable fftRunnable=new Runnable() {
        @Override
        public void run() {
//            byte[] bytes = new byte[fftNum];//16位即2比特一个数值，所以fft计算点为该大小/2
            HashMap<Double,Double> hmAllFre;
            ArrayDeque<Float> VoiceFreDeque=new ArrayDeque<>(80);
//            mAudioRecord.read(bytes, 0, bytes.length);//读取流
            List resList=FFT.fft(shorts);
            double maxFre= (double) resList.get(0);
            hmAllFre= (HashMap<Double, Double>) resList.get(1);
            Log.e(TAG,"hm大小"+hmAllFre.size());
            if (maxFre>=49 && maxFre<=500) {//过滤
                Log.e(TAG,"最大响度的频率："+maxFre);
                if (VoiceFreDeque.size()>=80) {
                    VoiceFreDeque.pop();
                }
                VoiceFreDeque.add((float) maxFre);
            }else {
                if (VoiceFreDeque.size()>=80) {
                    VoiceFreDeque.pop();
                }
                VoiceFreDeque.add((float) 0);
            }

            updateChartVoice(VoiceFreDeque);//时间-主频率图

//            updateChartFre(hmAllFre);//某次fft的频谱图
            if (mWhetherRecord) {
                audioHandler.postDelayed(fftRunnable,40);
            }
        }
    };
    Handler audioHandler=new Handler();

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
            case R.id.audio_act_btn_page_fre:
                Log.i(TAG,"频率页");
                initial1();

                break;
            case R.id.audio_act_btn_page_fft:
                Log.i(TAG,"频谱页");
                initial2();

                break;
            case R.id.audio_act_btn_page_anlay:
                Log.i(TAG,"分析页");

                break;
            case R.id.audio_act_btn_page_set:
                Log.i(TAG,"设置页");

                break;

        }
    }
}
