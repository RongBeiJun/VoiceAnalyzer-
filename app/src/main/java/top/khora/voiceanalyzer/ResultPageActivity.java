package top.khora.voiceanalyzer;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ResultPageActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_page_activity);
        initial();
    }
    private void initial(){
        TextView tv_botton_return=findViewById(R.id.analy_result_botton_tv_return);
        tv_botton_return.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.analy_result_botton_tv_return:
                finish();
                break;
        }
    }
}
