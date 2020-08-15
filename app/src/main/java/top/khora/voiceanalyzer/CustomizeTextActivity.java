package top.khora.voiceanalyzer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import top.khora.voiceanalyzer.Util.SharedPreferenceUtil;

public class CustomizeTextActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG="CustomizeTextActivity";
    private TextView tv_commit;
    private SharedPreferenceUtil spu;
    private EditText et_input_sentence;
    private String intentAction;
    private TextView tv_clear_article;
    private EditText et_input_article;
    private TextView tv_clear_sentence;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        intentAction = intent.getAction();
        if (intentAction.equals("sentence")) {
            Log.i(TAG,"Intent-Action:"+ intentAction);
            setContentView(R.layout.customize_sentence_layout);
            initial1();
        }else {//intentAction.equals("article")
            Log.i(TAG,"Intent-Action:"+ intentAction);
            setContentView(R.layout.customize_article_layout);
            initial2();
        }
    }
    private void initial1(){
        tv_commit = findViewById(R.id.tv_commit_setting);
        et_input_sentence = findViewById(R.id.et_input_comtomize_sentence);
        tv_commit.setOnClickListener(this);
        tv_clear_sentence = findViewById(R.id.tv_setting_clear_sentence);
        tv_clear_sentence.setOnClickListener(this);
        spu = new SharedPreferenceUtil(this);

    }
    private void initial2(){
        tv_commit = findViewById(R.id.tv_commit_setting_article);
        et_input_article = findViewById(R.id.et_input_comtomize_article);
        tv_commit.setOnClickListener(this);
        tv_clear_article = findViewById(R.id.tv_setting_clear_article);
        tv_clear_article.setOnClickListener(this);
        spu = new SharedPreferenceUtil(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_commit_setting:
                if (spu!=null) {
                    String et_string = et_input_sentence.getText().toString();
                    if (et_string.length()>5){
                        spu.putString("sentence",et_string);
                        Toast toast = Toast.makeText(CustomizeTextActivity.this,
                                null, Toast.LENGTH_SHORT);
                        toast.setText("自定义阅读短句设置成功！");
                        toast.show();
                    }else {
                        Toast toast = Toast.makeText(CustomizeTextActivity.this,
                                null, Toast.LENGTH_SHORT);
                        toast.setText("请输入超过5个字符~");
                        toast.show();
                    }

                }else {
                    Log.e(TAG,"SharedPreferenceUtil未创建");
                }

                break;
            case R.id.tv_commit_setting_article:
                if (spu!=null) {
                    String et_string = et_input_article.getText().toString();
                    if (et_string.length()>30){
                        spu.putString("article",et_string);
                        Toast toast = Toast.makeText(CustomizeTextActivity.this,
                                null, Toast.LENGTH_SHORT);
                        toast.setText("自定义阅读短文设置成功！");
                        toast.show();
                    }else {
                        Toast toast = Toast.makeText(CustomizeTextActivity.this,
                                null, Toast.LENGTH_SHORT);
                        toast.setText("请输入超过30个字符~");
                        toast.show();
                    }

                }else {
                    Log.e(TAG,"SharedPreferenceUtil未创建");
                }

                break;
            case R.id.tv_setting_clear_sentence:
                et_input_sentence.setText("");
                break;
            case R.id.tv_setting_clear_article:
                et_input_article.setText("");
                break;
        }

    }
}
