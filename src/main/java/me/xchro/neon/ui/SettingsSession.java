package me.xchro.neon.ui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.ConfigStore;
import net.minecraft.client.*;

/** One shared undo history across settings, exact-value dialogs, profiles and the HUD editor. */
public final class SettingsSession {
    private record State(String config, List<String> keys) {}
    private final Deque<State> undo = new ArrayDeque<>();
    private State before;
    private static List<KeyMapping> bindings() { return List.of(Neon.settings, Neon.freelook, Neon.zoom, Neon.autoclicker); }
    private State capture() { return new State(ConfigStore.encode(Neon.config), bindings().stream().map(KeyMapping::saveString).toList()); }
    public void begin() { if (before == null) before = capture(); }
    public void end() {
        if (before == null) return;
        State old = before; before = null;
        if (!old.equals(capture())) { undo.push(old); while (undo.size() > 50) undo.removeLast(); }
    }
    public void change(Runnable change) { begin(); try { change.run(); } finally { end(); } }
    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean undo() {
        end(); if (undo.isEmpty()) return false;
        State state = undo.pop();
        for (int i = 0; i < bindings().size(); i++) bindings().get(i).setKey(InputConstants.getKey(state.keys().get(i)));
        KeyMapping.resetMapping(); Minecraft.getInstance().options.save();
        Neon.applyConfig(ConfigStore.decode(state.config())); return true;
    }
    public void resetPage(int page) {
        change(() -> {
            switch (page) {
                case 0 -> Neon.config.resetCamera();
                case 1 -> Neon.config.resetClicker();
                case 2 -> Neon.config.resetHud();
                case 3 -> {
                    bindings().forEach(key -> key.setKey(key.getDefaultKey()));
                    KeyMapping.resetMapping(); Minecraft.getInstance().options.save();
                }
            }
        });
    }
}
