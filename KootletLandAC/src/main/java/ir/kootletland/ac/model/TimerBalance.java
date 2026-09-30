package ir.kootletland.ac.model;

/**
 * Tracks how much "game time" a client has consumed compared to real time.
 * A legitimate client sends one movement packet per 50ms tick, so the balance
 * stays near zero. A client running faster than 20 ticks/s (Timer) drifts up.
 * Lag spikes create a matching negative gap first, so the floor is generous.
 */
public final class TimerBalance {
    public static final double TICK_MS=50.0;
    private static final double FLOOR_MS=-1000.0;
    private double balance;
    private long lastNanos;
    private int samples;

    /** Records one movement packet and returns the current balance in ms. */
    public double tick(long nowNanos){
        if(lastNanos==0){lastNanos=nowNanos;samples=1;return balance;}
        double elapsedMs=Math.max(0,nowNanos-lastNanos)/1_000_000.0;
        lastNanos=nowNanos;
        samples++;
        balance+=TICK_MS-elapsedMs;
        if(balance<FLOOR_MS)balance=FLOOR_MS;
        return balance;
    }

    public void adjust(double ms){balance+=ms;}
    public double balance(){return balance;}
    public int samples(){return samples;}
    public void reset(){balance=0;lastNanos=0;samples=0;}
}
