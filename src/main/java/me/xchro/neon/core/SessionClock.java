package me.xchro.neon.core;

/** Connection lifetime, independent of wall-clock changes, game ticks and HUD visibility. */
public final class SessionClock {
    private long started;
    private boolean connected;
    public void connect(long now) { started=now;connected=true; }
    public void disconnect() { connected=false; }
    public long seconds(long now) { return connected?Math.max(0,now-started)/1_000_000_000L:0; }
    public static String format(long seconds) {
        seconds=Math.max(0,seconds);long hours=seconds/3600;long minutes=seconds/60%60;long remainder=seconds%60;
        return hours+":"+(minutes<10?"0":"")+minutes+":"+(remainder<10?"0":"")+remainder;
    }
}
