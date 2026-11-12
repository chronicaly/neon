package me.xchro.neon.ui;

import java.util.HashMap;
import java.util.Map;

/** Process-local navigation, deliberately excluded from configuration and profiles. */
final class MenuNavigation {
    private int page;
    private final Map<Integer, String> sections = new HashMap<>();
    private final Map<String, Double> positions = new HashMap<>();
    int page() { return page; }
    String section(int category) { return sections.getOrDefault(category, ""); }
    void select(int category, String section) { page = category; sections.put(category, section); }
    void remember(String view, double position) { positions.put(view, Math.max(0, position)); }
    double position(String view) { return positions.getOrDefault(view, 0.0); }
    void clear() { page = 0; sections.clear(); positions.clear(); }
}
