package ir.kootletland.ac.model;

import java.util.Arrays;

/** Fixed-size sliding window with the statistics the checks need (no cumulative drift). */
public final class StatsWindow {
    private final double[] values;
    private int size;
    private int next;

    public StatsWindow(int capacity){
        if(capacity<2)throw new IllegalArgumentException("capacity");
        values=new double[capacity];
    }

    public void add(double v){
        if(!Double.isFinite(v))return;
        values[next]=v;
        next=(next+1)%values.length;
        if(size<values.length)size++;
    }

    public int size(){return size;}
    public void clear(){size=0;next=0;}

    public double mean(){
        if(size==0)return 0;
        double sum=0;
        for(int i=0;i<size;i++)sum+=values[i];
        return sum/size;
    }

    public double variance(){
        if(size<2)return 0;
        double m=mean(),acc=0;
        for(int i=0;i<size;i++){double d=values[i]-m;acc+=d*d;}
        return acc/(size-1);
    }

    public double stdDev(){return Math.sqrt(variance());}

    public double coefficientOfVariation(){
        double m=mean();
        return m>0?stdDev()/m:0;
    }

    public double max(){
        double m=0;
        for(int i=0;i<size;i++)m=Math.max(m,values[i]);
        return m;
    }

    public double median(){return percentile(.5);}

    public double percentile(double p){
        if(size==0)return 0;
        double[] copy=Arrays.copyOf(values,size);
        Arrays.sort(copy);
        double rank=Math.max(0,Math.min(1,p))*(size-1);
        int lo=(int)Math.floor(rank),hi=(int)Math.ceil(rank);
        return copy[lo]+(copy[hi]-copy[lo])*(rank-lo);
    }

    /** Shannon entropy (bits) of the values bucketed by {@code binWidth}. Low entropy = suspiciously regular. */
    public double entropy(double binWidth){
        if(size==0||binWidth<=0)return 0;
        java.util.Map<Long,Integer> bins=new java.util.HashMap<>();
        for(int i=0;i<size;i++)bins.merge((long)Math.floor(values[i]/binWidth),1,Integer::sum);
        double h=0;
        for(int count:bins.values()){
            double p=count/(double)size;
            h-=p*(Math.log(p)/Math.log(2));
        }
        return h;
    }
}
