package top.khora.voiceanalyzer.Util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.ScatterChart;

public class DetailScatterChart extends ScatterChart {

    protected Paint mYAxisSafeZonePaint;
    final float INTERVAL = 20f;
    String[] arrayColors = {"#99EC2436","#99FEBF54","#992FB758","#99277BB7","#99E8E8E8"};
    String[] genderColors = {"#DAB8D0","#00ffffff","#5ACDF9"};
    int[] genderInterval={130,15,80};

    public DetailScatterChart(Context context) {
        super(context);
    }

    public DetailScatterChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DetailScatterChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();
        mYAxisSafeZonePaint = new Paint();
        mYAxisSafeZonePaint.setStyle(Paint.Style.FILL);

        mAxisRendererLeft=new CustomYAxisRendererLeft(mViewPortHandler
                ,mAxisLeft,mLeftAxisTransformer);//使用了重写的YleftRender
        mAxisRendererRight=new CustomYAxisRendererRight(mViewPortHandler
                ,mAxisRight,mRightAxisTransformer);//使用了重写的YRightRender
    }

    @Override
    protected void onDraw(Canvas canvas) {//重写以设置粉红、粉蓝背景

        float[] pts = new float[4];
        pts[1] = 310f;
        float tempPts ;
        for (int i = 0; i < 3; i++) {
            pts[3] = pts[1] - genderInterval[i];
            tempPts = pts[3];
            mLeftAxisTransformer.pointValuesToPixel(pts);
            mYAxisSafeZonePaint.setColor(Color.parseColor(genderColors[i]));
            canvas.drawRect(0f,
                    pts[1], mViewPortHandler.getChartWidth(), pts[3], mYAxisSafeZonePaint);
            pts[1] = tempPts;
        }

        super.onDraw(canvas);
    }
//    @Override
//    protected void onDraw(Canvas canvas) {
//
//        float[] pts = new float[4];
//        pts[1] = 100f;
//        float tempPts ;
//        for (int i = 0; i < 5; i++) {
//            pts[3] = pts[1] - INTERVAL;
//            tempPts = pts[3];
//            mLeftAxisTransformer.pointValuesToPixel(pts);
//            mYAxisSafeZonePaint.setColor(Color.parseColor(arrayColors[i]));
//            canvas.drawRect(mViewPortHandler.contentLeft(),
//                    pts[1], mViewPortHandler.contentRight(), pts[3], mYAxisSafeZonePaint);
//            pts[1] = tempPts;
//        }
//
//        super.onDraw(canvas);
//    }

    public void setSafeZoneColor(int color) {
        mYAxisSafeZonePaint.setColor(color);
    }
}
