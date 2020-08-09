package top.khora.voiceanalyzer.Util;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

public class AssetsUtil {
    public static String getAssetsToString(Context context, String fileName) {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final int bufferSize = 1024;
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        Reader in = null;
        try {
            in = new InputStreamReader(inputStream, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        for (; ; ) {
            int rsz = 0;
            try {
                rsz = in.read(buffer, 0, buffer.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (rsz < 0)
                break;
            out.append(buffer, 0, rsz);
        }
        return out.toString();
    }
}
