package top.khora.voiceanalyzer.Util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedPreferenceUtil {
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor editor;
    public SharedPreferenceUtil(Context context) {//SP名字以固定
        mSharedPreferences = context.getSharedPreferences("SharedPreferencesName", Context.MODE_PRIVATE);
        editor = mSharedPreferences.edit();
//        editor.putString(key, value);
//        editor.apply();//或commit
    }

    public void putString(String key,String value){
        if (editor==null){
            Log.e("SP","出错，SP的editor未找到!");
            return;
        }
        editor.putString(key, value);
        editor.apply();//或commit
    }
    public void removeKey(String key,String value){
        if (editor==null){
            Log.e("SP","出错，SP的editor未找到!");
            return;
        }
        editor.remove(key);
        editor.apply();//或commit
    }
    public String getString(String key,String defaultValue){
//        String s=mSharedPreferences.getString(key, defaultValue);
        return mSharedPreferences.getString(key, defaultValue);
    }
}
/**
 * 记录：
 * ResultPageActivity-replay_time
 * */
