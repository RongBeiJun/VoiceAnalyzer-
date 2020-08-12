package top.khora.voiceanalyzer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import top.khora.voiceanalyzer.Util.AssetsUtil;
import top.khora.voiceanalyzer.Util.DetailScatterChart;
import top.khora.voiceanalyzer.Util.FFT;
import top.khora.voiceanalyzer.Util.FileUtils;
import top.khora.voiceanalyzer.Util.SharedPreferenceUtil;

import static top.khora.voiceanalyzer.R.color.*;

public class AudioActivity extends AppCompatActivity implements View.OnClickListener,
        View.OnFocusChangeListener {
    private static String TAG="AudioActivity";
    public static String path;//暂时给FFT用来写测试数据
    private Button btn_open;
    private Button btn_stop;
    private Button btn_fre;
    private Button btn_fft;
    private Button btn_analy;
    private Button btn_set;
    private Button btn_replay;
    private Button btn_analy_reflesh;
    private Button btn_analy_toggle;
    private TextView tv_analy_article;
    private LineChart chart_fft;
    private DetailScatterChart chart_voice;

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
    private AudioTrack mAudioTrack;
    private int replayTime=5000;

    private TextView tv_botton_return;
    private EditText et_replayTime;
    private SharedPreferenceUtil spu;

    String articleTest;

    @Override
    protected void onStart() {
        super.onStart();
        //-TODO 应该在子线程中运行
        new Thread(){
            @Override
            public void run() {
                super.run();
                List<String> allFileNameInExternalFileFold =
                        FileUtils.getAllFileNameInFold(getExternalFilesDir("").getPath());
                for (int i=0;i<allFileNameInExternalFileFold.size();++i){
                    File file=new File(allFileNameInExternalFileFold.get(i));
                    Boolean isdeleted=file.delete();
                    Log.d(TAG+"-onStart","删除外部文件："+allFileNameInExternalFileFold.get(i)+
                            ":"+isdeleted);
                }
                List<String> allFileNameInExternalCacheFold =
                        FileUtils.getAllFileNameInFold(getExternalCacheDir().getPath());
                for (int i=0;i<allFileNameInExternalCacheFold.size();++i){
                    File file=new File(allFileNameInExternalCacheFold.get(i));
                    Boolean isdeleted=file.delete();
                    Log.d(TAG+"-onStart","删除缓存文件："+allFileNameInExternalCacheFold.get(i)+
                            ":"+isdeleted);
                }
            }
        }.start();

        spu = new SharedPreferenceUtil(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        path=getExternalFilesDir("").getPath();
        checkAuth();

        initMinBufferSize();
        initAudioRecord();
        startRecord();

        initial_before();
        initial1();
        initial_after();

        articleTest= AssetsUtil.getAssetsToString(this,"article");

        new Thread(){
            @Override
            public void run() {
                super.run();
                while (spu==null) {
                    getValueFromSPAndRendToWedgtForSetting();
                }
            }
        }.start();


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
                    //-TODO 设置新的界面能够让用户重启主动开启对应权限,需要进行测试
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
        btn_replay = findViewById(R.id.audio_act_btn_replay);
        btn_fre = findViewById(R.id.audio_act_btn_page_fre);
        btn_fft = findViewById(R.id.audio_act_btn_page_fft);
        btn_analy = findViewById(R.id.audio_act_btn_page_anlay);
        btn_set = findViewById(R.id.audio_act_btn_page_set);
        btn_analy_reflesh=findViewById(R.id.analy_reflesh_article_btn);
        btn_analy_toggle=findViewById(R.id.analy_toggle_btn);
        tv_analy_article=findViewById(R.id.analy_article_tv);
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
                btn_fre.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor_selected));
                btn_fft.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor));
                btn_analy.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor));
                btn_set.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor));
                pageNow=PAGE_1;
                break;
            case 2:
                linearLayout1.setVisibility(View.GONE);
                linearLayout2.setVisibility(View.VISIBLE);
                linearLayout3.setVisibility(View.GONE);
                linearLayout4.setVisibility(View.GONE);
                btn_fre.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor));
                btn_fft.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor_selected));
                btn_analy.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor));
                btn_set.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor));
                pageNow=PAGE_2;
                break;
            case 3:
                linearLayout1.setVisibility(View.GONE);
                linearLayout2.setVisibility(View.GONE);
                linearLayout3.setVisibility(View.VISIBLE);
                linearLayout4.setVisibility(View.GONE);
                btn_fre.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor));
                btn_fft.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor));
                btn_analy.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor_selected));
                btn_set.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor));
                pageNow=PAGE_3;
                break;
            case 4:
                linearLayout1.setVisibility(View.GONE);
                linearLayout2.setVisibility(View.GONE);
                linearLayout3.setVisibility(View.GONE);
                linearLayout4.setVisibility(View.VISIBLE);
                btn_fre.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor));
                btn_fft.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor));
                btn_analy.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor));
                btn_set.setTextColor(ContextCompat.getColor(this,bottom_btn_textColor_selected));
                pageNow=PAGE_4;
                break;

        }
    }
    private void initial1(){
        switchLayout(1);
        chart_voice = findViewById(R.id.chart_voice);
        if (!mWhetherRecord) {
            Log.i(TAG+"initial1","切换到fre页面必须进入播放运行");
            initAudioRecord();
            startRecord();
        }
    }
    private void initial2(){
        switchLayout(2);
        chart_fft = findViewById(R.id.chart_fft);
        if (!mWhetherRecord) {
            Log.i(TAG+"initial2","切换到fft页面必须进入播放运行");
            initAudioRecord();
            startRecord();
        }
    }
    private void initial3(){
        switchLayout(3);
        if (mWhetherRecord) {
            Log.i(TAG+"initial3","播放运行中,切换到分析页面需要断开");
            stopRecord();
        }
        tv_analy_article.setText(articleTest);
        //-TODO toggle状态还原 -Finished
        STATUS_analy_toggle_btn=0;
        btn_analy_toggle.setText("录制");

    }
    private void initial4(){
        switchLayout(4);
        et_replayTime = findViewById(R.id.et_setting_replaytime);
        et_replayTime.setOnFocusChangeListener(this);
        getValueFromSPAndRendToWedgtForSetting();

    }
    private void initial_after(){
        btn_open.setOnClickListener(this);
        btn_stop.setOnClickListener(this);
        btn_replay.setOnClickListener(this);
        btn_fre.setOnClickListener(this);
        btn_fft.setOnClickListener(this);
        btn_analy.setOnClickListener(this);
        btn_set.setOnClickListener(this);
        btn_analy_reflesh.setOnClickListener(this);
        btn_analy_toggle.setOnClickListener(this);
    }
    /**
     * 图表更新
     * */
    protected void updateChartFFTResult(HashMap hashMap){
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
        LineDataSet dataSet = new LineDataSet(entries, "频率分量"); // add entries to dataset
        dataSet.setColor(ContextCompat.getColor(this,R.color.testChart2));
        dataSet.setCircleColor(ContextCompat.getColor(this,R.color.testChart2));
        dataSet.setValueTextColor(ContextCompat.getColor(this,R.color.testChart1)); // styling, ...
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawCircles(false);
        dataSet.setDrawFilled(false);
        dataSet.setDrawValues(false);
        LineData scatterData = new LineData(dataSet);
        chart_fft.setData(scatterData);

        //移除默认描述
        Description desc=new Description();
        desc.setText("");
        chart_fft.setDescription(desc);

        XAxis xAxis= chart_fft.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMaximum(2400);
        xAxis.setAxisMinimum(20);

        YAxis yAxis= chart_fft.getAxisLeft();
        yAxis.setAxisMaximum(120);
        yAxis.setAxisMinimum(0);
        YAxis yAxis2= chart_fft.getAxisRight();
        yAxis2.setAxisMaximum(120);
        yAxis2.setAxisMinimum(0);

        yAxis.setDrawAxisLine(false);
        yAxis2.setDrawAxisLine(false);

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


        ScatterDataSet scatterDataSet = new ScatterDataSet(entries, "人声频率点"); // add entries to dataset
        scatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        scatterDataSet.setColor(ContextCompat.getColor(this,R.color.scatter_point));
        scatterDataSet.setValueTextColor(ContextCompat.getColor(this,R.color.scatter_point_text));
        //注：R.color.xxx为类型color.xml中对应颜色的index


//        scatterDataSet.setColors(new int[]{R.color.gender_female,R.color.gender_male},this);
//        scatterDataSet.setValueTextColors(new int[]{R.color.gender_female,R.color.gender_male},this);

        ScatterData scatterData = new ScatterData(scatterDataSet);
        chart_voice.setData(scatterData);
        chart_voice.setScaleXEnabled(false);
        chart_voice.setScaleYEnabled(false);
        //移除默认描述
        Description desc=new Description();
        desc.setText("");
        chart_voice.setDescription(desc);



        XAxis xAxis= chart_voice.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setEnabled(false);
        xAxis.setDrawAxisLine(false);//隐藏内部网格

        YAxis yAxis= chart_voice.getAxisLeft();
        yAxis.setAxisMaximum(500);
        yAxis.setAxisMinimum(0);
        yAxis.setLabelCount(11);
        YAxis yAxis2= chart_voice.getAxisRight();
        yAxis2.setAxisMaximum(500);
        yAxis2.setAxisMinimum(0);
        yAxis2.setLabelCount(11);
        yAxis.setDrawAxisLine(false);
        yAxis2.setDrawAxisLine(false);




        chart_voice.invalidate(); // refresh

    }


    /**
     * 一、初始化获取每一帧流的Size
     * */
    public static Integer mRecordBufferSize;
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
        /**
         * ENCODING_PCM_16BIT
         * Added in API level 3
         * public static final int ENCODING_PCM_16BIT
         * Audio data format: PCM 16 bit per sample. Guaranteed to be supported by devices.
         *
         * Constant Value: 2 (0x00000002)
         * 注：已经被转成-32768~32767的数，时序图形类余弦，直接用在fft中即可
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
    short[] shortsForreplay = new short[fftNum/2];
    private File pcmFileReplay;

    private void startRecord(){
        Log.i(TAG,"---startRecord---");
        fname= String.valueOf(new Date().getTime());
        pcmFile = new File(AudioActivity.this.getExternalCacheDir().getPath(),
                "audioRecord"+fname+".pcm");
        pcmFileReplay = new File(AudioActivity.this.getExternalCacheDir().getPath(),
                "audioRecordReplay.pcm");
        mWhetherRecord = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();//开始录制
                FileOutputStream fileOutputStream = null;
                FileOutputStream fileOutputStreamReplay = null;
                try {
//                    audioHandler.post(fftRunnable);
                    fileOutputStream = new FileOutputStream(pcmFile);
                    fileOutputStreamReplay = new FileOutputStream(pcmFileReplay);
//                    bytes = new byte[fftNum];//16位即2比特一个数值，所以fft计算点为该大小/2
                    shorts=new short[fftNum/2];
                    LinkedHashMap<Double,Double> hmAllFre;
                    ArrayDeque<Float> VoiceFreDeque=new ArrayDeque<>(80);
                    for (int i=0;i<80;++i){
                        VoiceFreDeque.add((float) 0);
                    }
                    double preMaxFre=0;
                    while (mWhetherRecord){
                        mAudioRecord.read(shorts, 0, shorts.length);//读取流
                        shortsForreplay=Arrays.copyOf(shorts,shorts.length);
                        //注意shorts是大端模式：低在低（前），高在高（后）
                        Log.i("BYTES--LENGTH",shorts.length+"");
                        List resList=FFT.fft(shorts);
                        double maxFre= (double) resList.get(0);
//                        double preMaxFre= (double) resList.get(0);//并不精准，使用了两次fft的均值
                        hmAllFre= (LinkedHashMap<Double, Double>) resList.get(1);
                        Log.d(TAG,"hm大小"+hmAllFre.size());
                        Log.d(TAG,"最大响度的频率："+maxFre);
                        if (maxFre>=49 && maxFre<=500) {//过滤
                            if (VoiceFreDeque.size()>=80) {
                                VoiceFreDeque.poll();
                            }
                            if ((maxFre+preMaxFre)/2>=49 && (maxFre+preMaxFre)/2<=500){
                                if (preMaxFre>=49 && preMaxFre<=500) {
                                    VoiceFreDeque.add((float) (maxFre+preMaxFre)/2);
                                }else {
                                    VoiceFreDeque.add((float) maxFre);
                                }
                            }
//                            VoiceFreDeque.add((float) maxFre);
                        }else {
                            if (VoiceFreDeque.size()>=80) {
                                VoiceFreDeque.poll();
                            }
                            VoiceFreDeque.add((float) 0);
                        }
                        preMaxFre=maxFre;

                        if (pageNow==PAGE_1) {
                            updateChartVoice(VoiceFreDeque);//时间-主频率图
                        }

                        if (pageNow==PAGE_2) {
                            updateChartFFTResult(hmAllFre);//某次fft的频谱图
                        }

//                        while (count*128<bytes.length) {
//                            FFT.fft(Arrays.copyOfRange(bytes,count*128,(count+1)*128));
//                            count++;
//                        }
                        fileOutputStream.write(short2byte(shorts));
                        fileOutputStream.flush();
                        fileOutputStreamReplay.write(short2byte(shortsForreplay));
                        fileOutputStreamReplay.flush();
                    }
                    Log.e(TAG, "run: 暂停录制" );
                    mAudioRecord.stop();//停止录制
                    fileOutputStreamReplay.flush();
                    fileOutputStreamReplay.close();
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
     * 分析用音频
     * */
    private boolean mWhetherRecord_Analy;
    private File pcmFile_Analy;
    private File pcmFileReplay_Analy;
    private String fname_Analy;
    //    byte[] bytes = new byte[fftNum];
    short[] shorts_Analy = new short[fftNum/2];
    short[] shortsForreplay_Analy = new short[fftNum/2];
    int femaleNum=0,maleNum=0;//180~310,80~165
    int otherNum=0,averageValue=0;
    float lowValue=0,highValue=0,midValue=0;
    ArrayDeque<Float> VoiceFreDequeAnaly=new ArrayDeque<>();
    private void startRecordAnaly(){
        Log.i(TAG,"---startRecordAnaly---");
        fname_Analy= String.valueOf(new Date().getTime());
        pcmFile_Analy = new File(AudioActivity.this.getExternalCacheDir().getPath(),
                "audioRecordAnaly"+fname_Analy+".pcm");
        pcmFileReplay_Analy = new File(AudioActivity.this.getExternalCacheDir().getPath(),
                "audioRecordReplayAnaly.pcm");
        mWhetherRecord_Analy = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                VoiceFreDequeAnaly.clear();//8-12，由于该容器外置，所以每次调用录音需要重置其内容
                mAudioRecord.startRecording();//开始录制
                FileOutputStream fileOutputStream = null;
                FileOutputStream fileOutputStreamReplay = null;
                try {
//                    audioHandler.post(fftRunnable);
                    fileOutputStream = new FileOutputStream(pcmFile_Analy);
                    fileOutputStreamReplay = new FileOutputStream(pcmFileReplay_Analy);
//                    bytes = new byte[fftNum];//16位即2比特一个数值，所以fft计算点为该大小/2
                    shorts_Analy=new short[fftNum/2];
                    LinkedHashMap<Double,Double> hmAllFre;
                    for (int i=0;i<80;++i){
                        VoiceFreDequeAnaly.add((float) 0);
                    }
                    double preMaxFre=0;
                    while (mWhetherRecord_Analy){
                        mAudioRecord.read(shorts_Analy, 0, shorts_Analy.length);//读取流
                        shortsForreplay_Analy=Arrays.copyOf(shorts_Analy,shorts_Analy.length);

//                        Log.i("BYTES--LENGTH",shorts_Analy.length+"");
                        List resList=FFT.fft(shorts_Analy);
                        double maxFre= (double) resList.get(0);
//                        double preMaxFre= (double) resList.get(0);//并不精准，使用了两次fft的均值
//                        hmAllFre= (LinkedHashMap<Double, Double>) resList.get(1);
//                        Log.d(TAG,"hm大小"+hmAllFre.size());
//                        Log.d(TAG,"最大响度的频率："+maxFre);
                        if (maxFre>=49 && maxFre<=500) {//过滤
                            if (VoiceFreDequeAnaly.size()>=80) {
//                                VoiceFreDeque.poll();//分析不设置容量上限
                            }
                            if ((maxFre+preMaxFre)/2>=49 && (maxFre+preMaxFre)/2<=500){
                                if (preMaxFre>=49 && preMaxFre<=500) {
                                    VoiceFreDequeAnaly.add((float) (maxFre+preMaxFre)/2);
                                }else {
                                    VoiceFreDequeAnaly.add((float) maxFre);
                                }
                            }
                        }else {
                            if (VoiceFreDequeAnaly.size()>=80) {
                                VoiceFreDequeAnaly.poll();
                            }
                            VoiceFreDequeAnaly.add((float) 0);
                        }
                        preMaxFre=maxFre;

                        fileOutputStream.write(short2byte(shorts_Analy));
                        fileOutputStream.flush();
                        fileOutputStreamReplay.write(short2byte(shortsForreplay_Analy));
                        fileOutputStreamReplay.flush();
                    }
                    Log.e(TAG, "run: 暂停录制" );
                    mAudioRecord.stop();//停止录制
                    fileOutputStreamReplay.flush();
                    fileOutputStreamReplay.close();
                    fileOutputStream.flush();
                    fileOutputStream.close();
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
    private void stopRecordAnaly(){
        Log.i(TAG,"---stopRecordAnaly---");
        mWhetherRecord_Analy = false;
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

//        File handlerWavFileReplay = new File(AudioActivity.this.getExternalCacheDir().getPath(),"audioRecordReplay_handler.wav");
//        pcmToWavUtil.pcmToWav(pcmFileReplay.toString(),handlerWavFileReplay.toString());
    }
    /**
     * 释放AudioRecord,录制流程完毕
     * 之后可在目录找到音频文件
     * */
    private void releaseAR(){
        Log.i(TAG,"---releaseAR---");
        mAudioRecord.release();
    }
    /**
     * 回放功能
     * */
    private void replay(final int replayTime){//输入单位为秒，播放最近的少于等于replayTime的音频
        Log.i(TAG+"-replay","---replay---");
        stopRecord();//暂停录音
        mAudioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                mRecordBufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);   //创建AudioTrack对象

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mAudioTrack!=null){
                    mAudioTrack.play();   //开始播放，此时播放的数据为空

                    if(pcmFile!=null){
                        Log.i(TAG+"-replay","pcmFile非空");
                        short[] tempShortBuffer;
                        byte[] tempByteBuffer = new byte[mRecordBufferSize*2];
//                        byte[] tempByteBuffer = new byte[replayTime*sampleRate*2];
                        try {
//                            FileInputStream fis=new FileInputStream(pcmFileReplay);
                            FileInputStream fis=new FileInputStream(pcmFile);
                            if (fis.available() > 0){
                                Log.i(TAG+"-replay","FileInputStream可获取");
                                int read=0;
                                while ((read = fis.read(tempByteBuffer))!= -1 ){
                                    if(read == AudioTrack.ERROR_INVALID_OPERATION ||
                                            read == AudioTrack.ERROR_BAD_VALUE ||
                                            read ==AudioTrack.ERROR) {
                                        Log.e(TAG+"-Replay","写入pcm出错");
                                        continue;
                                    }else{
//                                        Log.i(TAG+"-replay","read："+read);
                                        tempShortBuffer=byteArray2ShortArray(tempByteBuffer);
                                        if (fis.available()<replayTime*sampleRate*2) {
                                            mAudioTrack.write(tempShortBuffer,0,tempShortBuffer.length);  //将读取的数据写入到AudioTrack里面
                                        }
                                    }
                                }

                            }

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }catch (IOException e) {
                            e.printStackTrace();
                        }finally {

                            mAudioTrack.stop();
                            mAudioTrack.release();
                            Message msg=new Message();
                            msg.what=REOPEN_RECORD;
                            audioHandler.sendMessageDelayed(msg,500);
                        }
                    }
                }
            }
        }).start();

    }

    //convert short to byte
    public static byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }
    public static short[] byteArray2ShortArray(byte[] data) {
        short[] retVal = new short[data.length/2];
        for (int i = 0; i < retVal.length; i++)
            retVal[i] = (short) ((data[i * 2] & 0xff) | (data[i * 2 + 1] & 0xff) << 8);

        return retVal;
    }

    /**
     * handler
     * 之后可以用AsyncTract重写
     * */
    private static final int REOPEN_RECORD=1;
    private static final int DISABLE_ANALY_RECORD_ANALY=2;
    private static final int ENABLE_ANALY_RECORD_ANALY=3;
    Handler audioHandler=new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case REOPEN_RECORD:
                    Log.i(TAG+"handlerMessage","重开录音");
                    initAudioRecord();
                    startRecord();//-TODO 加判断防止开启时也执行
                    break;
                case DISABLE_ANALY_RECORD_ANALY:
                    Log.i(TAG+"handlerMessage","暂时使分析页的分析按钮失效");
                    btn_analy_toggle.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(AudioActivity.this,
                                    "录制时间过短，请录制至少5秒再进行分析",Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
                case ENABLE_ANALY_RECORD_ANALY:
                    Log.i(TAG+"handlerMessage","使分析页的分析按钮生效失效");
                    btn_analy_toggle.setOnClickListener(AudioActivity.this);
                    break;
            }

        }
    };
    /**
     * 设置页相关操作
     * */
    private void getValueFromSPAndRendToWedgtForSetting(){//app启动后需要执行和设置页开启后，以更新全局变量和渲染相关空间
        if (spu==null){
            Log.e(TAG,"未获取到spu对象");
            return;
        }
        String replay_time = spu.getString("replay_time", "");
        int replay_time_int=0;
        if (replay_time.length()>0){
            try {
                replay_time_int=Integer.parseInt(replay_time);
            } catch (NumberFormatException e) {
                Log.e(TAG,"获取到的重播时间格式有误，不进行渲染");
                e.printStackTrace();
            }
        }
        if (replay_time_int<5 || replay_time_int>25){
            Log.i(TAG,"获取到的重播时间获取失败或范围有误，不进行渲染");
        }else {
            Log.i(TAG,"渲染成功");
            et_replayTime.setText(String.valueOf(replay_time_int));
            replayTime=replay_time_int;
        }
    }

    private static int STATUS_analy_toggle_btn=0;//0时需要显示为播放，1时需要显示为停止并分析
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.audio_act_btn_open:
                Log.i(TAG,"开始录音-testBtn");
                initAudioRecord();
                startRecord();
                break;
            case R.id.audio_act_btn_stop:
                Log.i(TAG,"停止录音-testBtn");
                stopRecord();
                releaseAR();
                break;
            case R.id.audio_act_btn_replay:
                Log.i(TAG,"重播录音-testBtn");
                replay(replayTime/1000);//-TODO 重播时间 -Finished
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
                initial3();

                break;
            case R.id.audio_act_btn_page_set:
                Log.i(TAG,"设置页");
                initial4();
                break;
            case R.id.analy_reflesh_article_btn:
                Log.i(TAG,"分析页刷新文章");

                break;
            case R.id.analy_toggle_btn:
                Log.i(TAG,"分析页Toggle");
                if (STATUS_analy_toggle_btn==0) {
                    //-TODO 需要判断时间，太短则禁止并Toast
                    new Thread(){
                        @Override
                        public void run() {
                            super.run();
                            Message msgDisable=new Message();
                            msgDisable.what=DISABLE_ANALY_RECORD_ANALY;
                            Message msgEnable=new Message();
                            msgEnable.what=ENABLE_ANALY_RECORD_ANALY;
                            audioHandler.sendMessage(msgDisable);
                            audioHandler.sendMessageDelayed(msgEnable,5000);
                        }
                    }.start();
                    STATUS_analy_toggle_btn=1;
                    btn_analy_toggle.setText("分析");
                    initAudioRecord();
                    startRecordAnaly();
                }else {
                    STATUS_analy_toggle_btn=0;
                    btn_analy_toggle.setText("录制");
                    stopRecordAnaly();
                    //-TODO 分析并展示
                    /**
                     * 分析处理
                     * */
                    List<Float> VoiceFreList=new ArrayList<>(VoiceFreDequeAnaly);
                    Collections.sort(VoiceFreList);
                    femaleNum=0;maleNum=0;//180~310,80~165
                    otherNum=0;averageValue=0;lowValue=0;highValue=0;midValue=0;
                    long all=0;
                    for (int i=0;i<VoiceFreList.size();++i){
                        float value=VoiceFreList.get(i);
                        if (value<=49 || value>=320){
                            VoiceFreList.remove(i);
                            i--;
                            continue;
                        }
                    }
                    for (int i=0;i<VoiceFreList.size();++i){
                        float value=VoiceFreList.get(i);
                        if (value>=180 && value<=310){
                            femaleNum++;
                        }else if (value>=80 && value<=165){
                            maleNum++;
                        }
                        all+=value;
                    }
                    otherNum=VoiceFreList.size()-femaleNum-maleNum;
                    if (VoiceFreList.size()>0) {
                        averageValue= (int) (all/VoiceFreList.size());
                        lowValue=VoiceFreList.get((int)(VoiceFreList.size()*0.05));
                        highValue=VoiceFreList.get((int)(VoiceFreList.size()*0.95));
                        midValue=VoiceFreList.get((int)(VoiceFreList.size()*0.5));
                    }else {
                        averageValue= 0;
                        lowValue=0;
                        highValue=0;
                        midValue=0;
                    }

                    Log.i(TAG+"-beforeResultIntent","分析结果："+femaleNum+","+
                            maleNum+","+otherNum+","+lowValue+","+highValue+","+
                            averageValue+","+midValue);
                    Intent intent=new Intent(AudioActivity.this
                            ,ResultPageActivity.class);
                    intent.setAction("analyResult");
                    intent.putExtra("female",femaleNum);
                    intent.putExtra("male",maleNum);
                    intent.putExtra("other",otherNum);
                    intent.putExtra("low",lowValue);//float
                    intent.putExtra("high",highValue);//float
                    intent.putExtra("average",averageValue);
                    intent.putExtra("mid",midValue);//float
                    startActivity(intent);
                }
                break;

        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            // 此处为得到焦点时的处理内容
        } else {
            // 此处为失去焦点时的处理内容
            switch (v.getId()){
                case R.id.et_setting_replaytime:
                    int valueOfET=Integer.parseInt(et_replayTime.getText().toString());
                    if (valueOfET<5 || valueOfET>25){
                        Toast.makeText(this,"重播时间设置范围应该在5~25秒"
                                ,Toast.LENGTH_SHORT).show();
                        et_replayTime.setText("5");
                    }else {
                        spu.putString("replay_time",et_replayTime.getText().toString());
                        Toast t=Toast.makeText(this,null
                                ,Toast.LENGTH_SHORT);
                        String s="设置成功，重播时间为"+et_replayTime.getText().toString()+"秒";
                        t.setText("设置成功，重播时间为"+s);
                    }
                    break;
            }
        }

    }

}
