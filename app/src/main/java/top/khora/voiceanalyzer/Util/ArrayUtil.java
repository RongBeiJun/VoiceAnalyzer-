package top.khora.voiceanalyzer.Util;

import java.util.ArrayList;
import java.util.List;

public class ArrayUtil {
    public static List<Integer> arrayToList(int[] a){
        List<Integer> L=new ArrayList();
        for (int i:a){
            L.add(i);
        }
        return L;
    }
    public static List<String> arrayToList(String[] a){
        List<String> L=new ArrayList();
        for (String i:a){
            L.add(i);
        }
        return L;
    }
    public static List<Double> arrayToListDouble(double[] a){
        List<Double> L=new ArrayList();
        for (double i:a){
            L.add(i);
        }
        return L;
    }
    public static int[] ListToArray(List<Integer> L){
        int[] a=new int[L.size()];
        for (int i=0;i<L.size();++i){
            a[i]=L.get(i);
        }
        return a;
    }
    public static List<List<Integer>> arrayToList2Dimension(int[][] aa){
        List<List<Integer>> LL=new ArrayList();
        for (int i=0;i<aa.length;++i){
            LL.add(arrayToList(aa[i]));
        }
        return LL;
    }
    public static int[][] ListToArray2Dimension(List<List<Integer>> LL){
        int[][] aa=new int[LL.size()][LL.get(0).size()];
        for (int i=0;i<LL.size();++i){
            aa[i]=ListToArray(LL.get(i));
        }
        return aa;
    }
}
