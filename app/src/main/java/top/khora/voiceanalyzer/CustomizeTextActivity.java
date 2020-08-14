package top.khora.voiceanalyzer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class CustomizeTextActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG="CustomizeTextActivity";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String intentAction = intent.getAction();
        Log.i(TAG,"Intent-Action:"+intentAction);
        setContentView(R.layout.customize_text_layout);
        initial();
    }
    private void initial(){

    }

    @Override
    public void onClick(View v) {

    }
}
