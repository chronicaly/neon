package me.xchro.neon.config;

/** Unity's accent-derived surface roles, adapted to Neon's palette. */
public enum ThemeColor {
    BACKGROUND("Background", 0x030407, .045),
    OUTLINE("Outline", 0, 1),
    ACCENT("Accent", 0, 1),
    TEXT("Text", 0xffffff, .10),
    CARDS("Cards", 0x080b12, .035),
    SECONDARY("Secondary outline", 0, 1),
    MUTED("Muted text", 0xa8b3bd, .25);

    public final String label;
    private final int base;
    private final double tint;
    ThemeColor(String label, int base, double tint) { this.label = label; this.base = base; this.tint = tint; }
    public int resolve(int theme, int local) {
        int rgb = 0;
        for (int shift : new int[]{16, 8, 0}) {
            int channel = base >> shift & 255;
            rgb |= (int)Math.round(channel + ((theme >> shift & 255) - channel) * tint) << shift;
        }
        return (local & 0xff000000) | rgb;
    }
    public int local(NeonConfig.Theme theme) {
        return switch (this) {
            case BACKGROUND -> theme.background;
            case OUTLINE -> theme.outline;
            case ACCENT -> theme.accent;
            case TEXT -> theme.text;
            case CARDS -> theme.cards;
            case SECONDARY -> theme.secondary;
            case MUTED -> theme.muted;
        };
    }
    void local(NeonConfig.Theme theme, int color) {
        switch (this) {
            case BACKGROUND -> theme.background = color;
            case OUTLINE -> theme.outline = color;
            case ACCENT -> theme.accent = color;
            case TEXT -> theme.text = color;
            case CARDS -> theme.cards = color;
            case SECONDARY -> theme.secondary = color;
            case MUTED -> theme.muted = color;
        }
    }
}
