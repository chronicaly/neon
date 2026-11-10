package me.xchro.neon.core;

/** A toggle session belongs to the user, not to the current screen or world. */
public final class ClickerActivation {
    private boolean active, ready, previous;
    public boolean update(boolean enabled, boolean interactive, boolean down, boolean toggle) {
        if (!enabled) { reset(); return false; }
        if (!down) ready = true;
        if (interactive) {
            if (toggle) { if (ready && down && !previous) active = !active; }
            else active = ready && down;
        } else if (!toggle && !down) {
            active = false; // Releasing a Hold binding is an explicit stop.
        }
        previous = down;
        return active;
    }
    public void reset() { active = ready = previous = false; }
}
