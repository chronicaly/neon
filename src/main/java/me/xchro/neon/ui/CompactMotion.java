package me.xchro.neon.ui;

import java.util.*;
import java.util.function.Predicate;

/** Frame-rate independent motion; only the displayed geometry is interpolated. */
public final class CompactMotion {
    private final Map<String, Float> folds = new HashMap<>();
    private final Map<Object, Float> sliders = new HashMap<>();
    public void toggle(String key, boolean wasOpen) { folds.putIfAbsent(key, wasOpen ? 1f : 0f); }
    public void tick(Predicate<String> open, float dt, float speed, boolean enabled) {
        folds.replaceAll((key, value) -> move(value, open.test(key) ? 1 : 0, dt, speed, enabled));
    }
    public float amount(String key, boolean open) { return folds.getOrDefault(key, open ? 1f : 0f); }
    public boolean visible(String key, boolean open) { return open || amount(key, false) > 0; }
    public float slider(Object key, float target, float dt, boolean enabled) {
        float value = move(sliders.getOrDefault(key, target), target, dt, 24, enabled);
        sliders.put(key, value); return value;
    }
    public void clear() { folds.clear(); sliders.clear(); }
    private static float move(float value, float target, float dt, float speed, boolean enabled) {
        if (!enabled || Math.abs(value - target) < .002f) return target;
        return value + (target - value) * (1 - (float)Math.exp(-Math.max(0, speed) * Math.max(0, dt)));
    }
}
