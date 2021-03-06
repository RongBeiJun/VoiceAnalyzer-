package top.khora.voiceanalyzer.Util;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

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

    private static double preAvg=0;
    private static double thresholdRate=0.3;
    private static int filterSampleNum;
    public static List fft(short[] shorts) {
        filterSampleNum=0;
        List res=new ArrayList();
        LinkedHashMap<Double,Double> hm4AllFre=new LinkedHashMap<>();
        //创建傅里叶方法实例
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        double[] preFFT=new double[shorts.length];
        for (int i=0;i<shorts.length;++i){
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                preFFT[i]=Double.parseDouble(String.valueOf(toUnsignedInt(shorts[i])));
//            }else {
//                Log.e("FFT","版本低于安卓O，转码会失败");
//            }
            preFFT[i]=Double.parseDouble(String.valueOf(shorts[i]));//8-4:正确格式
//            preFFT[i]=Double.parseDouble(String.valueOf(toUnsignedInt(reverseByBit(shorts[i]))));
        }
        Complex[] result = fft.transform(preFFT, TransformType.FORWARD);
        Double[] Fres=new Double[result.length];
        Double[] zhenfus=new Double[result.length];
        StringBuilder sb=new StringBuilder();
        double rangeSum=0;
        double MaxSoundFre=0;
        double AllZhenfu=0;
        for(int i=0;i<AudioActivity.fftNumOfDrawPoints;i++){
            Fres[i]= (AudioActivity.sampleRate /(result.length))*(i)*1.0;//fft求频率公式
            zhenfus[i]=result[i].abs();
//            zhenfus[i]=Math.sqrt(Math.pow(result[i].getImaginary(),2)+Math.pow(result[i].getReal(),2));
            if (i==0){
                zhenfus[i]=zhenfus[i]/result.length;
            }else {
                zhenfus[i]=zhenfus[i]/(result.length/2);
            }
            zhenfus[i]=20*Math.log10(zhenfus[i]);
            if (i>0 && zhenfus[i]>rangeSum){//不使用第一个元素
                rangeSum=zhenfus[i];
                MaxSoundFre=Fres[i];
            }else {

            }
            if (Fres[i]>=49 && Fres[i]<=500) {
                AllZhenfu+=zhenfus[i];
                filterSampleNum++;
            }
            hm4AllFre.put(Fres[i],zhenfus[i]);
//            sb.append(Fres[i]+","+zhenfus[i]+","+result[i].getReal()+","+result[i].getImaginary()
//                    +","+preFFT[i]+","+shorts[i]+"\n");
//            Log.e("FFT傅里叶变换","第"+i+"个变换后频率为："+Fres[i]+"振幅："+zhenfus[i]);
//            System.out.println("第"+i+"个变换后数据为："+result[i]);
        }
//        FileUtils.writeLog(AudioActivity.path+"/voice-" + new Date().getTime()+".csv",sb.toString());
        /**
         * 过滤
         * */
        double thisAvg=AllZhenfu/filterSampleNum;
        if (rangeSum>45) {
            res.add(MaxSoundFre);
        }else {
            res.add(-0.1);
        }
        preAvg=thisAvg;
        /**
         * 不过滤
         * */
//        res.add(MaxSoundFre);

        res.add(hm4AllFre);
        return res;
    }

    public static double[] doubleMe(short[] pcms) {
        double[] doubles = new double[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            doubles[i] = pcms[i];
        }
        return doubles;
    }
    static private double[] ByteArray2DoubleArray(byte[] byteArray) {
        short[] out = new short[byteArray.length / 2]; // will drop last byte if odd number
        ByteBuffer bb = ByteBuffer.wrap(byteArray);
        for (int i = 0; i < out.length; i++) {
            out[i] = bb.getShort();
        }
        double[] pcmAsDoubles = doubleMe(out);
        return pcmAsDoubles;
//        double[] doubleArray=new double[byteArray.length/2];
//
//        for (int i = 0; i < doubleArray.length; i++) {
//            byte bl = byteArray[2 * i];
//            byte bh = byteArray[2 * i + 1];
//
//            short s = (short) ((bh & 0x00FF) << 8 | bl & 0x00FF);
//            /**
//             * Java中short是2字节 1字节是8bit 这里为什么要加上“& 0x00FF”呢？ 这是为了把复数前面的“很多个F”去掉
//             * 只取后8位的数据 防止相互影响
//             */
//
////            System.out.println("s_" + s);
//
//            doubleArray[i] = s / 32768f; // 32768 = 2^15
//        }
//        return doubleArray;
    }
    public static short byte2ToUnsignedShort(byte[] bytes, int off) {
        int high = bytes[off];
        int low = bytes[off + 1];
        return (short)(((high & 0x00FF) << 8) | (0x00FF & low));
    }
    public static double bytes2Double(byte[] arr) {
        int needToPadding0=8-arr.length;
        if (needToPadding0>0) {
            byte[] arr1=new byte[8];
            int i;
            for (i=0;i<needToPadding0;++i){
                arr1[i]=0;
            }
            for (;i<8;++i){
                arr1[i]=arr[i-needToPadding0];
            }
            arr=arr1;
        }
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long) (arr[i] & 0xff)) << (8 * i);
        }
        return Double.longBitsToDouble(value);
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
    public static int getUnsignedByte (byte data){      //将data字节型数据转换为0~255 (0xFF 即BYTE)。
        return data&0x0FF;
    }
    public static int getUnsignedShort(short data){
        return data&0xFFFF;
    }
    public static long getUnsignedInt(int data){
        return data&0xFFFFFF;
    }
    public static int toUnsignedInt(short x) {
        return ((int) x) & 0xffff;
    }


    public static short reverseByBit(short x) {
        short temp=0;
        for (int i = 0; i < 8; i ++)
        {
            temp = (short) (temp << 1);
            temp |= (x >> i) & 0x01;
        }

        return temp;
    }
}
