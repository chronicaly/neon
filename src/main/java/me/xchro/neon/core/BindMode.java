package me.xchro.neon.core;

/** Activation belongs to the bind; it is never repeated in a settings drawer. */
public enum BindMode {
    TOGGLE("Toggle", "T"), HOLD("Hold", "H"), REVERSE_HOLD("Reverse hold", "RH");
    public final String label, mark;
    BindMode(String label, String mark) { this.label = label; this.mark = mark; }
    public BindMode next() { return values()[(ordinal() + 1) % values().length]; }
}
