package me.xchro.neon.core;

/** Menu-only hysteresis. Frame intervals include the user's frame limiter. */
public final class AdaptiveEffects {
    private long last, warmUntil, changed, over, under;
    private int tier;
    public int tier() { return tier; }
    public static double budgetNanos(int refresh, int cap) { return 1e9 / Math.max(1, Math.min(refresh > 0 ? refresh : 60, cap > 0 ? cap : Integer.MAX_VALUE)); }
    public void frame(long now, double budget, boolean active, boolean full) {
        if (full) { tier = 0; changed = now; over = under = 0; }
        if (!active || full) { last = 0; return; }
        if (last == 0) { last = now; warmUntil = now + 1_000_000_000L; over = under = 0; return; }
        long elapsed = now - last; last = now;
        if (now < warmUntil || elapsed <= 0 || elapsed > 250_000_000L) { over = under = 0; return; }
        if (elapsed > budget * 1.15) { over += elapsed; under = 0; }
        else if (elapsed < budget * 1.08) { under += elapsed; over = 0; }
        else { over = under = 0; }
        if (now - changed < 3_000_000_000L) return;
        if (over >= 1_200_000_000L && tier < 2) { tier++; changed = now; over = under = 0; }
        else if (under >= 5_000_000_000L && tier > 0) { tier--; changed = now; over = under = 0; }
    }
}
