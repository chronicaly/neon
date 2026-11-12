package me.xchro.neon.core;

/** A toggle session belongs to the user, not to the current screen or world. */
public final class ClickerActivation {
    private boolean active, ready, previous;
    private BindMode mode;
    public boolean update(boolean enabled, boolean interactive, boolean down, boolean toggle) {
        return update(enabled, interactive, down, toggle ? BindMode.TOGGLE : BindMode.HOLD);
    }
    public boolean update(boolean enabled, boolean interactive, boolean down, BindMode nextMode) {
        if (mode != nextMode) { reset(); mode = nextMode; }
        if (!enabled) { reset(); return false; }
        if (!down) ready = true;
        if (interactive) {
            if (mode == BindMode.TOGGLE) { if (ready && down && !previous) active = !active; }
            else active = ready && (mode == BindMode.HOLD ? down : !down);
        } else if (mode != BindMode.TOGGLE && (mode == BindMode.HOLD ? !down : down)) {
            active = false; // Hold/reverse Hold can stop in a screen, but never start there.
        }
        previous = down;
        return active;
    }
    public void reset() { active = ready = previous = false; }
}
