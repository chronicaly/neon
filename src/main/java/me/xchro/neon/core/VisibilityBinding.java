package me.xchro.neon.core;

/** Passive modules retain their saved toggle; momentary modes never overwrite that preference. */
public final class VisibilityBinding {
    private boolean ready, previous;
    private BindMode mode;
    public record Result(boolean enabled, boolean active) {}
    public Result update(boolean enabled, boolean interactive, boolean bound, boolean down, BindMode nextMode) {
        if (mode != nextMode) { reset(); mode = nextMode; }
        if (!bound) { reset(); return new Result(enabled, enabled); }
        if (!interactive) {
            ready = false; previous = down;
            return new Result(enabled, mode == BindMode.TOGGLE && enabled);
        }
        if (!down) ready = true;
        if (ready && down && !previous && mode == BindMode.TOGGLE) enabled = !enabled;
        previous = down;
        return new Result(enabled, enabled && (mode == BindMode.TOGGLE || ready && (mode == BindMode.HOLD ? down : !down)));
    }
    public void reset() { ready = previous = false; }
}
