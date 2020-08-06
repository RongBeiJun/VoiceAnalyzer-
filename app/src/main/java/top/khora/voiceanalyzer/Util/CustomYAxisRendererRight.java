package top.khora.voiceanalyzer.Util;

import android.graphics.Canvas;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.renderer.YAxisRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

class CustomYAxisRendererRight extends YAxisRenderer {
    public CustomYAxisRendererRight(ViewPortHandler viewPortHandler, YAxis yAxis, Transformer trans) {
        super(viewPortHandler, yAxis, trans);
    }
    @Override
    protected void drawYLabels(Canvas c, float fixedPosition, float[] positions, float offset) {
        // draw
        for (int i = 0; i < mYAxis.mEntryCount; i++) {

            String text = mYAxis.getFormattedLabel(i)+"Hz";//新增Hz，自定义Y标签

            if (!mYAxis.isDrawTopYLabelEntryEnabled() && i >= mYAxis.mEntryCount - 1)
                return;
            if(i==mYAxis.mEntryCount-1) {//新增：为了让最上面的Y轴lable value显示到图表里面
                offset= Utils.calcTextHeight(mAxisLabelPaint, "Hz") / 1.5f +Math.abs(offset);
            }
            c.drawText(text, fixedPosition-23f, positions[i * 2 + 1] + offset, mAxisLabelPaint);
        }
    }
}
