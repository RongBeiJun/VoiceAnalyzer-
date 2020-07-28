package top.khora.voiceanalyzer.Util;

import android.util.Log;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.Date;

import top.khora.voiceanalyzer.AudioActivity;
/*
 * @description:快速傅里叶变换
 * */

public class FFT{
    public static void main(String[] args){
        //定义输入数据类型，并初始化
        double[] inputData = null;
        //定义数组长度
        int arrayLength = 4 * 1024;
        inputData = new double[arrayLength];
        for (int index = 0; index < inputData.length; index++){
            //将经过运算的随机数赋值给inputdata
            inputData[index] = (Math.random() - 0.5) * 100.0;
        }
        System.out.println("初始化完成");
        //创建傅里叶方法实例
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] result = fft.transform(inputData, TransformType.FORWARD);
        //将傅里叶变换前数据和变换后数据打印出来，显示前200
        for(int i=0;i<200;i++){
            System.out.print("第"+i+"个变换前数据为："+inputData[i]+"\t");
            System.out.println("第"+i+"个变换后数据为："+result[i]);
        }
    }

    public static double fft(byte[] bytes) {
        //创建傅里叶方法实例
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        double[] preFFT=new double[bytes.length];
        for (int i=0;i<bytes.length;++i){
            preFFT[i]=Double.parseDouble(String.valueOf(bytes[i]));
        }
        Complex[] result = fft.transform(preFFT, TransformType.FORWARD);
        Double[] Fres=new Double[result.length];
        Double[] zhenfus=new Double[result.length];
//        StringBuilder sb=new StringBuilder();
        double zongzhenfu=0;
        double MaxSoundFre=0;
        for(int i=0;i<4096;i++){
            Fres[i]=8000/(result.length)*(i+1)*1.0;//fft求频率公式
//            zhenfus[i]=(i==0?(result[i].getReal()/result.length): (result[i].getReal()/(result.length/2)));
            zhenfus[i]=Math.sqrt(Math.pow(result[i].getReal(),2)+Math.pow(result[i].getImaginary(),2));
            if (i==0){
                zhenfus[i]=zhenfus[i]/result.length;
            }else {
                zhenfus[i]=zhenfus[i]/(result.length/2);
            }
            if (Math.abs(zhenfus[i])>zongzhenfu){
                zongzhenfu=Math.abs(zhenfus[i]);
                MaxSoundFre=Fres[i];
            }else {

            }
//            sb.append(Fres[i]+","+zhenfus[i]+"\n");
//            Log.e("FFT傅里叶变换","第"+i+"个变换后频率为："+Fres[i]+"振幅："+zhenfus[i]);
//            System.out.println("第"+i+"个变换后数据为："+result[i]);
        }
//        FileUtils.writeLog(AudioActivity.path+"/voice-" + new Date().getTime()+".csv",sb.toString());
        return MaxSoundFre;
    }
    public static double[] bytesArr2DoubleArr(byte[] arr) {
        long value = 0;
        int dLength=arr.length/8;
        int kernalCount=0;
        double[] res=new double[dLength];
        int dCount=0;
        for (int i=0;i<arr.length;++i){
            if (kernalCount<=8){
                kernalCount++;
                value |= ((long) (arr[i] & 0xff)) << (8 * kernalCount);
            }else {
                res[dCount++]=Double.longBitsToDouble(value);
                kernalCount=0;
                value=0;
                i--;
                continue;
            }
        }
//        for (int i = 0; i < 8; i++) {
//            value |= ((long) (arr[i] & 0xff)) << (8 * i);
//        }
        return res;
    }
}
