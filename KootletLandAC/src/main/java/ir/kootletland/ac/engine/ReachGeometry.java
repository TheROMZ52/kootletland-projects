package ir.kootletland.ac.engine;

/** Pure geometry helpers for reach / hit-angle checks. */
public final class ReachGeometry {
    private ReachGeometry(){}

    public static double distanceToBox(double px,double py,double pz,
                                       double minX,double minY,double minZ,
                                       double maxX,double maxY,double maxZ){
        double dx=px-clamp(px,minX,maxX);
        double dy=py-clamp(py,minY,maxY);
        double dz=pz-clamp(pz,minZ,maxZ);
        return Math.sqrt(dx*dx+dy*dy+dz*dz);
    }

    /** Angle in degrees between two vectors, 0 when either has no length. */
    public static double angleDegrees(double ax,double ay,double az,double bx,double by,double bz){
        double la=Math.sqrt(ax*ax+ay*ay+az*az),lb=Math.sqrt(bx*bx+by*by+bz*bz);
        if(la<1e-9||lb<1e-9)return 0;
        double cos=(ax*bx+ay*by+az*bz)/(la*lb);
        return Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,cos))));
    }

    public static double clamp(double v,double min,double max){return Math.max(min,Math.min(max,v));}
}
