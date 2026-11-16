package me.xchro.neon.hud;

public final class HudNumbers {
    /** Unity HudTextContent: compact comma-separated coordinates and inline dimension conversion. */
    public static String coordinates(double x,double y,double z,double factor) {
        String actual=(long)Math.floor(x)+", "+(long)Math.floor(y)+", "+(long)Math.floor(z);
        return factor==1?actual:actual+" ("+(long)Math.floor(x*factor)+", "+(long)Math.floor(z*factor)+")";
    }
    private HudNumbers() {}
    /** Floor after conversion, including the west/north side of the origin. */
    public static long coordinate(double value, double factor) { return (long)Math.floor(value * factor); }
    public static double bearing(double yaw) { return ((yaw+180)%360+360)%360; }
    public static double angleDifference(double a,double b) { return ((a-b+540)%360+360)%360-180; }
    public static double horizontalSpeed(double dx,double dz,double ticksPerSecond) {
        double distance=Math.hypot(dx,dz);
        // Treat discontinuities as teleports, not movement samples.
        return Double.isFinite(distance)&&distance<=16&&Double.isFinite(ticksPerSecond)&&ticksPerSecond>0 ? distance*ticksPerSecond : 0;
    }
    public static String heading(double yaw) { return DIRECTIONS[(int)Math.floor((bearing(yaw)+22.5)/45)%8]; }
    public static String worldTime(long ticks) {
        int minutes=(int)(Math.floorMod(Math.floorMod(ticks,24000)+6000,24000)*1440/24000);
        return String.format(java.util.Locale.ROOT,"%02d:%02d",minutes/60,minutes%60);
    }
    private static final String[] DIRECTIONS={"N","NE","E","SE","S","SW","W","NW"};
}
