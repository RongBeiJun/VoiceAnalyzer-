package top.khora.voiceanalyzer;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import top.khora.voiceanalyzer.Util.SharedPreferenceUtil;

public class ResultPageActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG="ResultPageActivity";
//    private TextView tv_botton_return;
//    private EditText et_replayTime;
//    private SharedPreferenceUtil spu;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_page_activity);
        initial();
    }
    private void initial(){

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
