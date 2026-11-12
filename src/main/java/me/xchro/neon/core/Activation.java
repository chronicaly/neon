package me.xchro.neon.core;

/** A suspended action must see a released key before it can activate again. */
public final class Activation {
    private boolean ready, previous, toggled;
    private BindMode mode;
    public boolean update(boolean safe, boolean down, boolean toggle) {
        return update(safe, down, toggle ? BindMode.TOGGLE : BindMode.HOLD);
    }
    public boolean update(boolean safe, boolean down, BindMode nextMode) {
        if (mode != nextMode) { reset(); mode = nextMode; }
        if (!safe) { reset(); return false; }
        if (!down) ready = true;
        if (ready && down && !previous && mode == BindMode.TOGGLE) toggled = !toggled;
        previous = down;
        return ready && switch (mode) { case TOGGLE -> toggled; case HOLD -> down; case REVERSE_HOLD -> !down; };
    }
    public void reset() { ready = previous = toggled = false; }
}
