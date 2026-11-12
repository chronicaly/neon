package me.xchro.neon.core;

import java.util.Objects;

/** Input-thread cue arbitration, independent of Minecraft and the audio device. */
public final class UiSoundPolicy {
    public enum Cue {
        HOVER(0, .35), CONFIRM(1, 1), NAVIGATE(2, 1), BACK(2, 1), ENABLE(3, 1), DISABLE(3, 1), CHAT_MESSAGE(3, 1), CHAT_MENTION(3, 1);
        final int priority;
        final double gain;
        Cue(int priority, double gain) { this.priority = priority; this.gain = gain; }
    }
    public record Request(Cue cue, double volume, long time) {}
    private int depth;
    private Cue pending;
    private String target;
    private long lastHover, lastAction;
    private boolean hovered, acted;
    public void begin() { depth++; }
    public Request end(long now, double volume) {
        if (--depth != 0) return null;
        Cue cue = pending; pending = null;
        return cue == null ? null : request(cue, now, volume);
    }
    public void remember(String key) { target = key; }
    public Request hover(String key, long now, double volume) {
        if (Objects.equals(target, key)) return null;
        target = key;
        return key == null ? null : request(Cue.HOVER, now, volume);
    }
    public Request request(Cue cue, long now, double volume) {
        if (depth > 0) {
            if (pending == null || cue.priority >= pending.priority) pending = cue;
            return null;
        }
        if (cue == Cue.HOVER) {
            if (hovered && now - lastHover < 80_000_000L || acted && now - lastAction < 100_000_000L) return null;
            hovered = true; lastHover = now;
        } else { acted = true; lastAction = now; }
        if (!Double.isFinite(volume) || volume <= 0) return null;
        return new Request(cue, Math.min(1, volume) * cue.gain, now);
    }
}
