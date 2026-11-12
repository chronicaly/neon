package me.xchro.neon.config;

/** Optional per-widget overrides; absent values keep following the saved menu theme. */
public enum FpsColor {
    LABEL("Label color", ThemeColor.MUTED),
    VALUE("Value color", ThemeColor.TEXT),
    BACKGROUND("Background color", ThemeColor.BACKGROUND),
    BORDER("Border color", ThemeColor.OUTLINE);

    public final String label;
    private final ThemeColor themeRole;
    FpsColor(String label, ThemeColor themeRole) { this.label = label; this.themeRole = themeRole; }
    public int color(NeonConfig config) { return config.fpsColors.getOrDefault(this, config.theme.color(themeRole)); }
    public boolean synced(NeonConfig config) { return !config.fpsColors.containsKey(this); }
    public void color(NeonConfig config, int value) { config.fpsColors.put(this, value); }
    public void sync(NeonConfig config) {
        if (synced(config)) color(config, color(config)); else config.fpsColors.remove(this);
    }
}
