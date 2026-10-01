package ir.kootletland.ac.model;

/**
 * Looks at pitch deltas. Real mouse input is quantized to a grid derived from the player's sensitivity;
 * rotations computed by software (aim assist / kill aura) usually are not.
 * This is a supporting signal only: zoom mods, touch input and pitch clamping all break the grid.
 */
public final class RotationAnalyzer {
    private static final double MIN_GRID_STEP=.03;
    private static final double GRID_TOLERANCE=.004;
    private final double[] pitch;
    private final double[] yawAccel;
    private int pitchSize,pitchNext,accelSize,accelNext;
    private double lastYawDelta;

    public RotationAnalyzer(int capacity){
        pitch=new double[capacity];
        yawAccel=new double[capacity];
    }

    public void add(double yawDelta,double pitchDelta){
        if(!Double.isFinite(yawDelta)||!Double.isFinite(pitchDelta))return;
        double a=Math.abs(pitchDelta);
        if(a>.0005){pitch[pitchNext]=a;pitchNext=(pitchNext+1)%pitch.length;if(pitchSize<pitch.length)pitchSize++;}
        yawAccel[accelNext]=Math.abs(yawDelta-lastYawDelta);
        accelNext=(accelNext+1)%yawAccel.length;
        if(accelSize<yawAccel.length)accelSize++;
        lastYawDelta=yawDelta;
    }

    public int samples(){return pitchSize;}

    /** Mean absolute yaw acceleration. Humans are jerky; very low values mean unnaturally smooth rotation. */
    public double smoothness(){
        if(accelSize==0)return 0;
        double s=0;
        for(int i=0;i<accelSize;i++)s+=yawAccel[i];
        return s/accelSize;
    }

    /** Estimated sensitivity grid step, or 0 when no consistent grid exists (or it cannot be judged). */
    public double gridStep(){
        if(pitchSize<12)return 0;
        double min=Double.MAX_VALUE;
        for(int i=0;i<pitchSize;i++)min=Math.min(min,pitch[i]);
        if(min<MIN_GRID_STEP)return 0;
        for(int div=1;div<=6;div++){
            double step=min/div;
            if(step<MIN_GRID_STEP)break;
            if(fits(step))return step;
        }
        return 0;
    }

    /** True only when there is enough data, the deltas are large enough to judge, and they fit no grid at all. */
    public boolean quantizationBroken(){
        if(pitchSize<20)return false;
        double min=Double.MAX_VALUE;
        for(int i=0;i<pitchSize;i++)min=Math.min(min,pitch[i]);
        if(min<MIN_GRID_STEP)return false;
        return gridStep()==0;
    }

    private boolean fits(double step){
        int ok=0;
        for(int i=0;i<pitchSize;i++){
            double r=pitch[i]/step;
            if(Math.abs(r-Math.rint(r))*step<GRID_TOLERANCE)ok++;
        }
        return ok>=pitchSize*.9;
    }
}
