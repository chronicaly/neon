package me.xchro.neon.core;

/** A suspended action must see a released key before it can activate again. */
public final class Activation {
    private boolean ready, previous, toggled;
    public boolean update(boolean safe, boolean down, boolean toggle) {
        if (!safe) { reset(); return false; }
        if (!down) ready = true;
        if (ready && down && !previous && toggle) toggled = !toggled;
        previous = down;
        return ready && (toggle ? toggled : down);
    }
    public void reset() { ready = previous = toggled = false; }
}
