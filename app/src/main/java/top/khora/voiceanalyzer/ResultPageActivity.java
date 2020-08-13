package top.khora.voiceanalyzer;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import top.khora.voiceanalyzer.Util.FileUtils;
import top.khora.voiceanalyzer.Util.SharedPreferenceUtil;

import static top.khora.voiceanalyzer.R.color.resultFloatBtnPress;

public class ResultPageActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG="ResultPageActivity";
    private TextView tv_return;
    private PieChart analyResultPiechart;
    int femaleNum=0,maleNum=0;//180~310,80~165
    int otherNum=0,averageValue=0;
    float lowValue=0,highValue=0,midValue=0;
    private FloatingActionButton fab;
    private TextView highTv;
    private TextView avgTv;
    private TextView midTv;
    private TextView lowTv;
    //    private TextView tv_botton_return;
//    private EditText et_replayTime;
//    private SharedPreferenceUtil spu;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_page_activity);
        initial();
        Intent intent=getIntent();
        if (intent.getAction().equals("analyResult")) {
            femaleNum=intent.getIntExtra("female",-1);
            maleNum=intent.getIntExtra("male",-1);
            otherNum=intent.getIntExtra("other",-1);
            lowValue=intent.getFloatExtra("low",-1.1f);
            highValue=intent.getFloatExtra("high",-1.1f);
            averageValue=intent.getIntExtra("average",-1);
            midValue=intent.getFloatExtra("mid",-1.1f);
        }
        Log.i(TAG,"接受到的参数："+femaleNum+"--"+maleNum+"--"+otherNum+"--"+lowValue
                +"--"+highValue+"--"+averageValue+"--"+midValue);
        initialPieChart();
        initialTv();

    }
    private void initial(){
        tv_return = findViewById(R.id.analyresult_botton_tv_return);
        tv_return.setOnClickListener(this);
        analyResultPiechart = findViewById(R.id.analyresult_piechart);
        fab = findViewById(R.id.analyresult_floatbutton);
        fab.setOnClickListener(this);
        highTv = findViewById(R.id.analyresult_tv_high);
        avgTv = findViewById(R.id.analyresult_tv_avg);
        midTv = findViewById(R.id.analyresult_tv_mid);
        lowTv = findViewById(R.id.analyresult_tv_low);
    }
    private void initialPieChart(){
        float femaleRate= 0;
        float maleRate= 0;
        float otherRate= 0;
        if (femaleNum+maleNum+otherNum>0) {
            femaleRate = 100*(float)femaleNum/(femaleNum+maleNum+otherNum);
            maleRate = 100*(float)maleNum/(femaleNum+maleNum+otherNum);
            otherRate = (100-maleRate-femaleRate);
        }
        Log.i(TAG,femaleRate+","+maleRate+","+otherRate);

        ArrayList<Integer> colors = new ArrayList<Integer>();
        List<PieEntry> entries = new ArrayList<PieEntry>();
        if (maleRate>0) {
            entries.add(new PieEntry(maleRate,"男真声"));
            colors.add(ContextCompat.getColor(this,R.color.gender_male));
        }
        if (femaleRate>0) {
            entries.add(new PieEntry(femaleRate,"女真声"));
            colors.add(ContextCompat.getColor(this,R.color.gender_female));
        }
        if (otherRate>0) {
            entries.add(new PieEntry(otherRate,"假声等"));
            colors.add(ContextCompat.getColor(this,R.color.myGrey));
        }
        if (maleRate==0 && femaleRate==0 && otherRate==0){
            entries.add(new PieEntry(40f,"未检测到人声频率"));
            colors.add(ContextCompat.getColor(this,R.color.gender_male));
            entries.add(new PieEntry(60f,"请检查声道或重新录制"));
            colors.add(ContextCompat.getColor(this,R.color.gender_female));
        }
//        entries.add(new PieEntry(20f,"男声频率"));
//        entries.add(new PieEntry(30f,"女声频率"));
//        entries.add(new PieEntry(50f,"Other"));
//        ArrayList<Integer> valuesColors = new ArrayList<Integer>();
//        colors.add(ContextCompat.getColor(this,R.color.white));
//        colors.add(ContextCompat.getColor(this,R.color.white));
//        colors.add(ContextCompat.getColor(this,R.color.black));

        PieDataSet pieDataSet=new PieDataSet(entries,"");
        pieDataSet.setColors(colors);
        pieDataSet.setFormSize(10f);


//        pieDataSet.setValueTextColors(valuesColors);

        PieData piedata=new PieData(pieDataSet);
        piedata.setDrawValues(true);
        piedata.setValueFormatter(new PercentFormatter(analyResultPiechart));
        piedata.setValueTextSize(10f);
        piedata.setValueTextColor(ContextCompat.getColor(this,R.color.white));

        Description desc=new Description();
        desc.setText("");
        analyResultPiechart.setDescription(desc);
        analyResultPiechart.setHoleRadius(0f);
        analyResultPiechart.setTransparentCircleRadius(0f);
        analyResultPiechart.setUsePercentValues(true);
        analyResultPiechart.setData(piedata);
        analyResultPiechart.invalidate();
    }
    private void initialTv(){
        highTv.setText(String.valueOf(highValue));
        avgTv.setText(String.valueOf(averageValue));
        midTv.setText(String.valueOf(midValue));
        lowTv.setText(String.valueOf(lowValue));
    }


    private AudioTrack mAudioTrack;
    private File pcmFileAnaly;
    private Boolean needToPlay=true;
    private Boolean isPlaying=false;
    private void replayForAnaly(){//输入单位为秒，播放最近的少于等于replayTime的音频
        Log.i(TAG,"---replay---");
//        stopRecord();//暂停录音
        mAudioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(AudioActivity.sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                AudioActivity.mRecordBufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);   //创建AudioTrack对象
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mAudioTrack!=null){
                    isPlaying=true;

                    mAudioTrack.play();   //开始播放，此时播放的数据为空
                    pcmFileAnaly=new File(ResultPageActivity.this.getExternalCacheDir().getPath(),
                            "audioRecordReplayAnaly.pcm");
                    if(pcmFileAnaly!=null){
                        Log.i(TAG,"pcmFile非空");
                        short[] tempShortBuffer;
                        byte[] tempByteBuffer = new byte[AudioActivity.mRecordBufferSize*2];
//                        byte[] tempByteBuffer = new byte[replayTime*sampleRate*2];
                        try {
//                            FileInputStream fis=new FileInputStream(pcmFileReplay);
                            FileInputStream fis=new FileInputStream(pcmFileAnaly);
                            if (fis.available() > 0){
                                Log.i(TAG,"FileInputStream可获取");
                                int read=0;
                                while (needToPlay && (read = fis.read(tempByteBuffer))!= -1 ){
                                    if(read == AudioTrack.ERROR_INVALID_OPERATION ||
                                            read == AudioTrack.ERROR_BAD_VALUE ||
                                            read ==AudioTrack.ERROR) {
                                        Log.e(TAG,"写入pcm出错");
                                        continue;
                                    }else{
//                                        Log.i(TAG+"-replay","read："+read);
                                        tempShortBuffer=AudioActivity
                                                .byteArray2ShortArray(tempByteBuffer);
                                        if (fis.available()>0) {
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
                            isPlaying=false;
                            mAudioTrack.stop();
                            mAudioTrack.release();
                            fab.setRippleColor(ContextCompat.getColor(ResultPageActivity.this
                                    , resultFloatBtnPress));
                            fab.setImageResource(android.R.drawable.ic_media_play);
                        }
                    }
                }
            }
        }).start();

    }

    private void deleteReplayFile(){//onDestory调用删除本次分析的pcm文件
        Log.i("TAG","删除本次分析的pcm文件,需要由结果页面的活动onDestory调用");
        pcmFileAnaly.delete();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deleteReplayFile();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.analyresult_botton_tv_return:
                finish();
                break;
            case R.id.analyresult_floatbutton:
                if (!isPlaying) {//不在播放时按
                    needToPlay=true;
                    replayForAnaly();
                    fab.setRippleColor(ContextCompat.getColor(this,R.color.resultFloatBtn));
                    fab.setImageResource(android.R.drawable.ic_media_pause);
                }else {//在播放时按
                    needToPlay=false;
                }
                break;
        }
    }

}
