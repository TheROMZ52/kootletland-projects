package ir.kootletland.ac.model;

/**
 * Vanilla vertical motion model, free of any server types so it can be tested with scenarios.
 * Per tick: y += vy; vy = (vy - 0.08) * 0.98. The tracker adopts the observed velocity every tick, so a legitimate
 * arc stays at ~0 deviation while hovering or rising shows up as a steady positive deviation.
 */
public final class VerticalMotionTracker {
    public static final double GRAVITY=.08;
    public static final double DRAG=.98;
    public static final double JUMP_VELOCITY=.42;
    public static final double THRESHOLD=.05;
    public static final int WINDOW=8;
    public static final int REQUIRED=6;

    private final double[] deviations=new double[WINDOW];
    private int count;
    private int next;
    private double predicted;

    public static double nextVelocity(double velocity){return (velocity-GRAVITY)*DRAG;}

    public double predicted(){return predicted;}
    public void predicted(double v){predicted=v;}

    public void clear(){count=0;next=0;}

    /** A server-applied velocity (knockback, wind charge...) restarts the model from that velocity. */
    public void applyVelocity(double vy){predicted=vy;clear();}

    public void push(double deviation){
        deviations[next]=deviation;
        next=(next+1)%WINDOW;
        if(count<WINDOW)count++;
    }

    public int samples(){return count;}

    public int above(double threshold){
        int n=0;
        for(int i=0;i<count;i++)if(deviations[i]>threshold)n++;
        return n;
    }

    public boolean sustainedUpward(){return count>=REQUIRED&&above(THRESHOLD)>=REQUIRED;}

    /**
     * @param skip true when the model does not apply this tick (graced, lag, liquids, vines...)
     * @return this tick's deviation, or NaN when the model did not apply
     */
    public double step(double actualDy,boolean serverOnGround,boolean wasServerOnGround,boolean skip,double jumpBonus){
        if(serverOnGround){predicted=0.0;clear();return Double.NaN;}
        if(skip){predicted=nextVelocity(actualDy);clear();return Double.NaN;}
        double deviation;
        if(wasServerOnGround){
            // first airborne tick: a jump (up to jump velocity) or just walking off a ledge
            deviation=Math.max(0,actualDy-(JUMP_VELOCITY+jumpBonus+.05));
        }else{
            deviation=actualDy-predicted;
        }
        push(deviation);
        predicted=nextVelocity(actualDy);
        return deviation;
    }
}
